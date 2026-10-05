package kt.fluxo.tests

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.container
import kt.fluxo.core.debug.debugIntentLabel
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** kotlin-reflect is on the test classpath, so a rendered lambda signature would show up here as `... -> ...`. */
internal class DebugIntentLabelTest : CoroutineScopeAwareTest() {

    private data class Discrete(val id: Int)

    @Test
    fun other_intents_keep_their_toString() {
        assertEquals("Discrete(id=1)", debugIntentLabel(Discrete(1)))
        assertEquals("null", debugIntentLabel(null))
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
