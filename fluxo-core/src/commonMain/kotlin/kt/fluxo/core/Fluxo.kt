@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "TooManyFunctions", "TrailingCommaOnCallSite")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.FluxoStore
import kt.fluxo.core.internal.ReducerIntentHandler
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.js.JsName
import kotlin.jvm.JvmName


// Convenience DSL for Fluxo usage


// region CoroutineScope extensions for Store

/**
 * Returns MVVM+ [Store], connected to [CoroutineScope] lifecycle.
 */
@InlineOnly
public inline fun <State> CoroutineScope.container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, Nothing>, State, Nothing>.() -> Unit = {},
): Container<State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.container(initialState, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

/**
 * Returns MVVM+ [Store] with [SideEffect] support, connected to [CoroutineScope] lifecycle.
 */
@InlineOnly
@LowPriorityInOverloadResolution
@JsName("scopedContainerWithSideEffects")
@JvmName("containerWithSideEffects")
public inline fun <State, SideEffect : Any> CoroutineScope.container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.container<State, SideEffect>(initialState, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

/**
 * Returns basic [Reducer]-based [Store], connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> CoroutineScope.store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    @BuilderInference settings: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
): Store<Intent, State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL, connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> CoroutineScope.store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
    @BuilderInference settings: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
): Store<Intent, State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.store(initialState, handler, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL and [SideEffect] support, connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
@JsName("scopedStoreWithSideEffects")
@JvmName("storeWithSideEffects")
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.store(initialState, handler, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

// endregion


// region Basic Store variants

/**
 * Returns MVVM+ [Store].
 */
@InlineOnly
public inline fun <State> container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, Nothing>, State, Nothing>.() -> Unit = {},
): Container<State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return container<State, Nothing>(initialState) {
        sideEffectsStrategy = SideEffectsStrategy.DISABLE
        settings()
    }
}

/**
 * Returns MVVM+ [Store] with [SideEffect] support.
 */
@InlineOnly
@LowPriorityInOverloadResolution
@JsName("containerWithSideEffects")
@JvmName("containerWithSideEffects")
public inline fun <State, SideEffect : Any> container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(
        initialState,
        @Suppress("UNCHECKED_CAST") (FluxoIntentHandler as IntentHandler<FluxoIntent<State, SideEffect>, State, SideEffect>),
        settings,
    )
}

/**
 * Returns basic [Reducer]-based [Store].
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    @BuilderInference settings: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
): Store<Intent, State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
    @BuilderInference settings: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
): Store<Intent, State, Nothing> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return FluxoStore(
        initialState = initialState,
        intentHandler = handler,
        conf = FluxoSettings<Intent, State, Nothing>().apply {
            sideEffectsStrategy = SideEffectsStrategy.DISABLE
            settings()
        },
    )
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL and [SideEffect] support.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
@JsName("storeWithSideEffects")
@JvmName("storeWithSideEffects")
public inline fun <Intent, State, SideEffect : Any> store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return FluxoStore(
        initialState = initialState,
        intentHandler = handler,
        conf = FluxoSettings<Intent, State, SideEffect>().apply(settings),
    )
}

// endregion


// region Store DSL

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store].
 */
@FluxoDsl
@InlineOnly
public inline fun <S, SE : Any> ContainerHost<S, SE>.intent(noinline intent: FluxoIntent<S, SE>): Unit = container.send(intent)

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store].
 */
@FluxoDsl
@InlineOnly
public inline fun <S, SE : Any> Container<S, SE>.intent(noinline intent: FluxoIntent<S, SE>): Unit = send(intent)

/**
 *
 *
 * NOTE: Works only for the standart implementation of Fluxo [Store] ([FluxoStore]).
 */
@ExperimentalFluxoApi
public suspend fun Store<*, *, *>.closeAndWait() {
    close()
    val store = this as FluxoStore
    store.interceptorScope.coroutineContext[Job]!!.join()
    store.intentContext[Job]!!.join()
}

/**
 *
 * @see StoreScope.sideJob
 */
@FluxoDsl
@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun <I, S, SE : Any> StoreScope<I, S, SE>.repeatOnSubscription(
    key: String = StoreScope.DEFAULT_REPEAT_ON_SUBSCRIPTION_JOB,
    stopTimeout: Long = 100L,
    block: suspend SideJobScope<I, S, SE>.() -> Unit,
) {
    sideJob(key) {
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
                block()
            }
        }
    }
}

// endregion
