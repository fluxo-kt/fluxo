package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kt.fluxo.core.annotation.FluxoDsl

@FluxoDsl
public interface SideJobScope<in Intent, out State, in SideEffect : Any> : CoroutineScope {

    public val currentStateWhenStarted: State

    public val restartState: RestartState

    @Suppress("DeferredIsResult")
    public suspend fun postIntent(intent: Intent): Deferred<Unit>

    public suspend fun postSideEffect(sideEffect: SideEffect)

    public enum class RestartState {
        Initial, Restarted
    }
}
