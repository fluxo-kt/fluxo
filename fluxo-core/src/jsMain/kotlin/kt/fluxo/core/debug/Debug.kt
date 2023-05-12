@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "KotlinRedundantDiagnosticSuppress",
    "NOTHING_TO_INLINE",
)

package kt.fluxo.core.debug

import kt.fluxo.common.annotation.InlineOnly


// this::class.qualifiedName reflection API is not supported yet in JavaScript.
@InlineOnly
internal actual inline fun Any.debugClassName(): String? = this::class.simpleName
