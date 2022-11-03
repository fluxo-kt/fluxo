package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.annotation.InternalFluxoApi

@FluxoDsl
@InternalFluxoApi
public interface SideJobScope<in Intent, State, in SideEffect : Any> : CoroutineScope {

    public val currentStateWhenStarted: State

    public val restartState: RestartState

    public suspend fun updateState(block: (State) -> State): State

    public suspend fun postIntent(intent: Intent): Job

    public suspend fun postSideEffect(sideEffect: SideEffect)

    public enum class RestartState {
        Initial, Restarted
    }
}
