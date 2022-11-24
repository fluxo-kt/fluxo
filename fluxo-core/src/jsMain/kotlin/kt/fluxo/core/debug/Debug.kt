@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.debug

import kotlin.internal.InlineOnly


// this::class.qualifiedName reflection API is not supported yet in JavaScript.
@InlineOnly
internal actual inline fun Any.debugClassName(): String? = this::class.simpleName
