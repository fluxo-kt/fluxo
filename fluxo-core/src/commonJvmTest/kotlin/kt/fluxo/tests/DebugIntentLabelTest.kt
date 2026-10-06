package kt.fluxo.tests

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.debugIntentLabel
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Lambda intents and values they capture are labelled by class name: rendering them via kotlin-reflect is slow. */
internal class DebugIntentLabelTest : CoroutineScopeAwareTest() {

    private data class Discrete(val id: Int)

    @Test
    fun other_intents_keep_their_toString() {
        assertEquals("Discrete(id=1)", debugIntentLabel(Discrete(1)))
    }

    @Test
    fun property_reference_is_labelled_by_class() {
        val ref = Discrete::id
        assertEquals(ref.javaClass.name, debugIntentLabel(ref))
    }

    /**
     * With DEBUG on (pure JVM), `send` wraps the intent and its label lists captured values by class name, including
     * a lambda held in a captured `var` (boxed in a `Ref.ObjectRef`). Without DEBUG (Android) nothing is wrapped.
     */
    @Test
    fun debug_coroutine_name_of_sent_intent_labels_captured_lambdas_by_class() = runUnitTest {
        if (!DEBUG) return@runUnitTest
        var name: String? = null
        var callback: suspend () -> Unit = {}
        fun capturing(): FluxoIntent<String, Nothing> = {
            callback()
            name = currentCoroutineContext()[CoroutineName]?.name
            noOp()
        }
        val store = scope.container(INIT) { debugChecks = true }
        store.send(capturing()).join()
        val actual = assertNotNull(name)
        assertTrue("callback=${callback.javaClass.name}" in actual, actual)
    }

    /** The guardian's rejection message names a lambda intent by class, not by its reflection-rendered signature. */
    @Test
    fun guardian_rejection_names_lambda_intent_by_class() = runUnitTest {
        // Does nothing, so the guardian rejects it. Sent from the bootstrapper, it reaches the store unwrapped.
        val intent: FluxoIntent<String, Nothing> = {}
        val store = scope.container(INIT) {
            debugChecks = true
            bootstrapper = { send(intent) }
        }
        assertNotNull(store.start()).join()
        val job = assertNotNull(store.coroutineContext[Job])
        job.join()
        val messages = generateSequence<Throwable>(job.getCancellationException()) { it.cause }.mapNotNull { it.message }
        val actual = messages.joinToString(" | ")
        assertTrue("(intent=${intent.javaClass.name};" in actual, actual)
    }

    /** Sent from a bootstrapper, the intent reaches the store unwrapped (no FluxoIntentDebug), as without DEBUG. */
    @Test
    fun debug_coroutine_name_of_raw_lambda_uses_the_label() = runUnitTest {
        var name: String? = null
        val intent: FluxoIntent<String, Nothing> = {
            name = currentCoroutineContext()[CoroutineName]?.name
            noOp()
        }
        val store = scope.container(INIT) {
            debugChecks = true
            bootstrapper = { send(intent).join() }
        }
        assertNotNull(store.start()).join()
        val actual = assertNotNull(name)
        assertTrue(actual.endsWith("<= Intent ${intent.javaClass.name}]"), actual)
    }
}
