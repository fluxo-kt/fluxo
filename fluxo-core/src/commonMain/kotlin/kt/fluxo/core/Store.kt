package kt.fluxo.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.internal.Closeable
import kt.fluxo.core.internal.StoreClosedException
import kotlin.js.JsName

/**
 * Convenience typealias for an MVVM+ Fluxo [Store] setup.
 */
public typealias Container<State, SideEffect> = Store<FluxoIntent<State, SideEffect>, State, SideEffect>

@ThreadSafe
public interface Store<in Intent, out State, out SideEffect : Any> : Closeable {

    public val name: String

    public val stateFlow: StateFlow<State>

    public val sideEffectFlow: Flow<SideEffect>

    public val isActive: Boolean


    /**
     *
     * @throws StoreClosedException
     */
    @JsName("send")
    public fun send(intent: Intent)

    /**
     *
     * @throws StoreClosedException
     */
    @JsName("sendAsync")
    public suspend fun sendAsync(intent: Intent): Job


    /**
     *
     * @throws StoreClosedException
     */
    @JsName("start")
    public fun start()

    public override fun close()
}
