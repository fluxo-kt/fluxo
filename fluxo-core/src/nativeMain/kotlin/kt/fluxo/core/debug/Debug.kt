@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.debug

import kt.fluxo.common.annotation.InlineOnly

@InlineOnly
internal actual inline fun Any.debugClassName(): String? = this::class.qualifiedName
