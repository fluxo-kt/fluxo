package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.annotation.FluxoDsl

@FluxoDsl
public interface SideJobScope<in Intent, out State, in SideEffect : Any> : CoroutineScope {

    public val currentStateWhenStarted: State

    public val restartState: RestartState

    public suspend fun postIntent(intent: Intent)

    public suspend fun postSideEffect(sideEffect: SideEffect)

    public enum class RestartState {
        Initial, Restarted
    }
}
