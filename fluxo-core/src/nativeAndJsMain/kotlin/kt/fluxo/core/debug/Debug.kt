@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "KotlinRedundantDiagnosticSuppress",
    "NOTHING_TO_INLINE",
)

package kt.fluxo.core.debug

import kt.fluxo.common.annotation.InlineOnly

// TODO: Split debug and release version
internal actual val DEBUG: Boolean = false

/** No native implementation */
@InlineOnly
internal actual inline fun <I> debugIntentWrapper(intent: I): I = intent
