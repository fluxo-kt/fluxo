@file:Suppress("MemberVisibilityCanBePrivate", "MaxLineLength")

package kt.fluxo.core.data

import kotlinx.atomicfu.atomic
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.common.annotation.InternalFluxoApi
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.internal.Closeable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Wrapper for a [side effect][T] that can guarantee that [content] handled exactly once.
 * [handleOrResend] function acknowledges successful processing between a producer and a consumer,
 * and resends this side effect through the [Store] otherwise.
 *
 * Together with [Store] it can also guarantee delivery of the side effect.
 */
public open class GuaranteedEffect<out T : Any>(
    /** Raw effect data. Use [content] instead for "exactly once" guarantees! */
    @InternalFluxoApi
    public val rawContent: T,
) : Closeable {
    /*
     * Due to the prompt cancellation guarantee changes that landed in Coroutines 1.4,
     * Channels and Flows cannot guarantee delivery and even more so, they don't have the API to acknowledge
     * successful processing between a producer and consumer to guarantee that an item handled exactly once.
     *
     * #### References
     *
     * [Proposal] Primitive or Channel that guarantees the delivery and processing of items
     * https://github.com/Kotlin/kotlinx.coroutines/issues/2886
     *
     * Support leak-free closeable resources transfer via Channel (onUndeliveredElement parameter)
     * https://github.com/Kotlin/kotlinx.coroutines/issues/1936
     * https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/#undelivered-elements
     *
     * Rethink atomicity of certain low-level primitives
     * https://github.com/Kotlin/kotlinx.coroutines/issues/1813
     *
     * Shared flows, broadcast channels
     * https://elizarov.medium.com/shared-flows-broadcast-channels-899b675e805c
     * by Roman Elizarov [Nov 16, 2020]
     *
     * https://gmk57.medium.com/unfortunately-events-may-be-dropped-if-channel-receiveasflow-cfe78ae29004
     * https://gist.github.com/gmk57/330a7d214f5d710811c6b5ce27ceedaa
     *
     * LiveData with SnackBar, Navigation and other events (the SingleLiveEvent case)
     * https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
     * https://gist.github.com/JoseAlcerreca/5b661f1800e1e654f07cc54fe87441af
     * by Jose Alcérreca [Apr 27, 2018]
     */

    private val resendFun = atomic<((sideEffect: Any?) -> Unit)?>(null)

    private val hasBeenHandled = atomic(false)


    /**
     * Returns [raw effect data][T] or `null` if already handled.
     */
    public val content: T?
        get() = if (hasBeenHandled.compareAndSet(expect = false, update = true)) rawContent else null

    /**
     * Convenience method that provides "exactly once" handling guarantees.
     * 1. Takes [raw effect data][content] or returns if already handled.
     * 2. Calls the [handle] function.
     * 3. [Clears][close] the effect resources if handled successfully. [Resends][resend] effect otherwise.
     *
     * You can use all this methods by yourself if this method not suites you well enough.
     *
     * @param handle function to handle the [raw effect data][content].
     *  Return `true` if handled successfully, `false` if the effect should be resent.
     *
     * @return `true` if handled successfully, `false` otherwise.
     */
    @ExperimentalFluxoApi
    public inline fun handleOrResend(handle: (content: T) -> Boolean): Boolean {
        contract {
            callsInPlace(handle, InvocationKind.AT_MOST_ONCE)
        }
        var handled = false
        val content = content ?: return false
        try {
            handled = handle(content)
        } finally {
            if (!handled) resend() else close()
        }
        return handled
    }

    /**
     * Resend this effect through the [Store] again and marks it as unhandled.
     */
    @ExperimentalFluxoApi
    public fun resend() {
        val f = checkNotNull(resendFun.value) { "resend is possible only after first publication" }
        hasBeenHandled.value = false
        f(this)
    }

    /**
     * Clears connection to the [Store] required for [resending][resend] possibility.
     */
    @CallSuper
    @ExperimentalFluxoApi
    public override fun close() {
        resendFun.value = null
    }


    /**
     * Called internally to set [connection][block] to the [Store] required for [resending][resend] possibility.
     */
    @InternalFluxoApi
    internal fun <S> setResendFunction(block: ((sideEffect: S) -> Unit)?) {
        @Suppress("UNCHECKED_CAST")
        resendFun.value = block as ((Any?) -> Unit)?
    }
}
