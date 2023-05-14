@file:JvmName("FluxoDsl")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.RunningSideJob.Companion.DEFAULT_REPEAT_ON_SUBSCRIPTION_JOB
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.JsName
import kotlin.jvm.JvmName


// Convenience DSL for Fluxo usage (non-inline)


/**
 * Closes the [Store] and suspends the invoking coroutine until the [Store] is completely finished.
 *
 * @see kotlinx.coroutines.cancelAndJoin
 */
@ExperimentalFluxoApi
@JsName("closeStoreAndWait")
@JvmName("closeStoreAndWait")
public suspend fun Store<*, *>.closeAndWait() {
    close()
    coroutineContext[Job]?.join()
}

/**
 *
 * NOTE: `wasRestarted` is `true` in the [block] when the
 * [repeatOnSubscription] called second time with the same [key].
 *
 * @see StoreScope.sideJob
 */
@FluxoDsl
@JsName("repeatOnSubscriptionIn")
@JvmName("repeatOnSubscriptionIn")
@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun <I, S, SE : Any> StoreScope<I, S, SE>.repeatOnSubscription(
    key: String = DEFAULT_REPEAT_ON_SUBSCRIPTION_JOB,
    stopTimeout: Long = 100L,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: SideJob<I, S, SE>,
) {
    sideJob(key = key, context = context, start = start) { wasRestarted ->
        /**
         * @see kotlinx.coroutines.flow.map
         * @see kotlinx.coroutines.flow.mapLatest
         * @see kotlinx.coroutines.flow.distinctUntilChanged
         * @see kotlinx.coroutines.flow.DistinctFlowImpl
         * @see kotlinx.coroutines.flow.collectLatest
         */
        // TODO: Optimize logic for performance and less allocations, benchmark
        val upstream = this@repeatOnSubscription.subscriptionCount
        if (stopTimeout > 0L) {
            upstream.mapLatest {
                if (it > 0) {
                    true
                } else {
                    delay(stopTimeout)
                    false
                }
            }
        } else {
            upstream.map { it > 0 }
        }.distinctUntilChanged().collectLatest { subscribed ->
            if (subscribed) {
                block(wasRestarted)
            }
        }
    }
}
