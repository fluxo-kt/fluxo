package kt.fluxo.test.interception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

internal abstract class InterceptionBase {

    fun test(creations: Int = INTERCEPTORS_INIT_COUNT, interceptions: Int = INTERCEPTIONS_COUNT, interceptors: Int = INTERCEPTORS_COUNT): Int =
        runBlocking {
            val state = MutableStateFlow(0)
            repeat(creations) {
                state.value = 0
                test(interceptions, interceptors, state)
            }
            state.value
        }

    protected abstract suspend fun test(interceptions: Int, interceptors: Int, state: MutableStateFlow<Int>)
}
