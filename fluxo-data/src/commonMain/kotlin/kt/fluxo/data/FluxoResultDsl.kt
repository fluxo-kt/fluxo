@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "KotlinRedundantDiagnosticSuppress",
    "NOTHING_TO_INLINE",
    "TooManyFunctions",
)

package kt.fluxo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kt.fluxo.common.annotation.InlineOnly
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.js.JsExport


// region resultOf

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] that was thrown from the [block] function execution (except [CancellationException]!) and
 * encapsulating it as a failure.
 *
 * @see kotlin.runCatching
 */
@JsExport
@InlineOnly
public inline fun <R> resultOf(block: () -> R): FluxoResult<R?> {
    return try {
        FluxoResult.success(block())
    } catch (ce: CancellationException) {
        // Don't break cooperative concurrency
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1814
        throw ce
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        FluxoResult.failure(e)
    }
}

/**
 * Calls the specified function [block] with `this` value as its receiver.
 * Returns an encapsulated result if invocation was successful.
 * Catches any [Throwable] from the [block] function execution (except [CancellationException]!) and
 * encapsulating it as a [failure].
 *
 * @see kotlin.runCatching
 */
@InlineOnly
@LowPriorityInOverloadResolution
public inline fun <T, R> T.resultOf(block: T.() -> R): FluxoResult<R?> {
    return try {
        FluxoResult.success(block())
    } catch (ce: CancellationException) {
        // Don't break cooperative concurrency
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1814
        throw ce
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        (this as? FluxoResult<*>)?.error?.let { e.addSuppressed(it) }
        FluxoResult.failure(e)
    }
}

// endregion


// region extensions

/**
 * Throws exception if the result is failure. Minimizes inlined bytecode for [getOrThrow].
 *
 * @see kotlin.throwOnFailure
 */
@PublishedApi
internal fun FluxoResult<*>.throwOnFailure() {
    if (isFailure) throw error ?: error("Expected value, got $this")
}

/**
 * Returns the encapsulated value if this instance represents [non-failure][FluxoResult.isFailure]
 * otherwise throws the encapsulated [Throwable].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 *
 * @see kotlin.getOrThrow
 */
@JsExport
@InlineOnly
public inline fun <T> FluxoResult<T>.getOrThrow(): T {
    throwOnFailure()
    return value
}

/**
 * Returns the encapsulated value if this instance represents [success][FluxoResult.isSuccess] or the
 * result of [onFailure] for the encapsulated [Throwable] if it is a [failure][FluxoResult.isFailure].
 *
 * Note that this function rethrows any [Throwable] thrown by [onFailure] function.
 *
 * This function is a shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 *
 * @see kotlin.getOrElse
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.getOrElse(onFailure: (exception: Throwable?) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return if (isFailure) onFailure(error) else value
}

/**
 * Returns the encapsulated value if this instance represents [non-empty state][FluxoResult.isEmpty] or the
 * result of [defaultValue] otherwise. *
 *
 * Note that this function rethrows any [Throwable] thrown by [defaultValue] function.
 *
 * @see kotlin.getOrDefault
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T?>.getOrDefault(defaultValue: () -> R): R {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty || value == null) defaultValue() else value
}

/**
 * Returns the result of [onSuccess] for the encapsulated value if this instance represents [success][FluxoResult.isSuccess]
 * or the result of [onFailure] function for the encapsulated [Throwable] if it is [failure][FluxoResult.isFailure].
 *
 * Note that this function rethrows any [Throwable] thrown by [onSuccess] or by [onFailure] function.
 *
 * @see kotlin.fold
 */
@JsExport
@InlineOnly
public inline fun <R, T> FluxoResult<T>.fold(onSuccess: (value: T) -> R, onFailure: (exception: Throwable?) -> R): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isFailure -> onFailure(error)
        else -> onSuccess(value)
    }
}


/**
 * Returns the result of the [predicate] on the encapsulated value.
 */
@JsExport
@InlineOnly
public inline fun <T> FluxoResult<T>.isValid(predicate: (T) -> Boolean): Boolean {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return predicate(value)
}

// endregion


// region mapping

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value,
 * non-value state of the original result is saved.
 *
 * Note that this function rethrows any [Throwable] thrown by [transform] function.
 *
 * See [mapCatching] for encapsulating exceptions alternative.
 *
 * @see kotlin.map
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.map(transform: (T) -> R): FluxoResult<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return if (!isFailure) FluxoResult.success(transform(value)) else this
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value,
 * non-value state of the original result is saved.
 *
 * This function catches any [Throwable] that was thrown by [transform] function (except [CancellationException]!) and it as a failure.
 *
 * See [map] for rethrowing exceptions alternative.
 *
 * @see resultOf
 * @see kotlin.mapCatching
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.mapCatching(transform: (value: T) -> R): FluxoResult<R?> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        map(transform)
    } catch (ce: CancellationException) {
        // Don't break cooperative concurrency
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1814
        throw ce
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        error?.let { e.addSuppressed(it) }
        FluxoResult.failure(e)
    }
}


/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable]
 * if this instance represents [failure][FluxoResult.isFailure] or the original encapsulated value otherwise.
 *
 * Note that this function rethrows any [Throwable] thrown by [transform] function.
 *
 * See [recoverCatching] for encapsulating exceptions alternative.
 *
 * @see kotlin.recover
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.recover(transform: (exception: Throwable?) -> R): FluxoResult<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return if (isFailure) FluxoResult.success(transform(error)) else this
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable]
 * if this instance represents [failure][FluxoResult.isFailure] or the original encapsulated value otherwise.
 *
 * This function catches any [Throwable] that was thrown by [transform] function (except [CancellationException]!) and it as a failure.
 *
 * See [recover] for rethrowing exceptions alternative.
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.recoverCatching(transform: (exception: Throwable?) -> R): FluxoResult<R?> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        recover(transform)
    } catch (ce: CancellationException) {
        // Don't break cooperative concurrency
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1814
        throw ce
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        error?.let { e.addSuppressed(it) }
        FluxoResult.failure(e)
    }
}


@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.cached(value: R = this.value): FluxoResult<R> = FluxoResult.cached(value)

@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.loading(value: R = this.value): FluxoResult<R> = FluxoResult.loading(value)

/**
 *
 * @see FluxoResult.recover
 * @see kotlin.recover
 */
@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.success(value: R = this.value): FluxoResult<R> = FluxoResult.success(value)

@JsExport
@InlineOnly
public inline fun <R, T : R> FluxoResult<T>.failure(error: Throwable? = this.error, value: R = this.value): FluxoResult<R> =
    FluxoResult.failure(error, value)

// endregion


// region "peek" onto value/error and pipe

/**
 * Performs the given [action] on the encapsulated value if this instance represents [success][FluxoResult.isSuccess].
 * Returns the original result unchanged.
 */
@JsExport
@InlineOnly
public inline fun <T> FluxoResult<T>.onSuccess(action: (T) -> Unit): FluxoResult<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) {
        action(value)
    }
    return this
}

/**
 * Performs the given [action] on the encapsulated [Throwable] if this instance represents [failure][FluxoResult.isFailure].
 * Returns the original result unchanged.
 *
 * @see kotlin.onFailure
 */
@JsExport
@InlineOnly
public inline fun <T> FluxoResult<T>.onFailure(action: FluxoResult<T>.(Throwable?) -> Unit): FluxoResult<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isFailure) {
        action(error)
    }
    return this
}

// endregion


/**
 * Filters-out empty non-final states.
 */
public fun <T> Flow<FluxoResult<T>>.filterResult(): Flow<FluxoResult<T>> = filter { it.isFailed || it.isSuccess || !it.isEmpty }
