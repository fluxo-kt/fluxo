@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kt.fluxo.core.Store
import kt.fluxo.core.intercept.FluxoInterceptor
import kt.fluxo.core.intercept.StoreRequest
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.internal.InlineOnly
import kt.fluxo.core.dsl.InterceptorScope as Scope

internal class InterceptorScopeImpl<I, S, SE : Any>(
    override val store: Store<I, S, SE>,
    private val sendRequest: suspend (StoreRequest<I, S>) -> Unit,
    override val coroutineContext: CoroutineContext,
    internal val interceptors: Array<out FluxoInterceptor<I, S, SE>>,
) : Scope<I, S, SE> {

    override suspend fun postRequest(request: StoreRequest<I, S>) = sendRequest(request)


    internal suspend fun <T> intercept(
        value: T,
        call: suspend FluxoInterceptor<I, S, SE>.(Scope<I, S, SE>, T, proceed: suspend (T) -> Unit) -> Unit,
        finish: suspend (finalValue: T) -> Unit,
    ) {
        contract {
            callsInPlace(call, InvocationKind.UNKNOWN)
            callsInPlace(finish, InvocationKind.AT_MOST_ONCE)
        }
        // Fast path
        if (interceptors.isEmpty()) {
            finish(value)
        } else {
            Chain(value, this, call, finish).proceed()
        }
    }

    @InlineOnly
    internal inline fun intercept(block: FluxoInterceptor<I, S, SE>.(scope: Scope<I, S, SE>) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        for (interceptor in interceptors) {
            interceptor.run {
                block(this@InterceptorScopeImpl)
            }
        }
    }


    private class Chain<T, I, S, SE : Any>(
        value: T,
        private val scope: InterceptorScopeImpl<I, S, SE>,
        private val call: suspend FluxoInterceptor<I, S, SE>.(Scope<I, S, SE>, T, proceed: suspend (T) -> Unit) -> Unit,
        private val finish: suspend (finalValue: T) -> Unit,
    ) {
        private val index = MutableStateFlow(0)
        private val value = MutableStateFlow(value)

        suspend fun proceed() {
            val i = index.value
            val interceptors = scope.interceptors
            if (i >= interceptors.size) {
                finish(value.value)
                return
            }
            interceptors[i].run {
                call(scope, value.value) { v ->
                    value.value = v
                    index.getAndAdd(delta = 1)
                    proceed()
                }
            }
        }
    }
}
