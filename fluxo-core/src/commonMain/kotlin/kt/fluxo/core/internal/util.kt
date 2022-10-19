package kt.fluxo.core.internal

import kotlinx.coroutines.CancellationException

internal fun Throwable?.toCancellationException() = when (this) {
    null -> null
    is CancellationException -> this
    else -> CancellationException("", cause = this)
}
