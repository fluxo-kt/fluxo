@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.debug

import kotlin.internal.InlineOnly

// TODO: Split debug and release version
internal actual val DEBUG: Boolean = false

/** No native implementation */
@InlineOnly
internal actual inline fun <I> debugIntentWrapper(intent: I): I = intent
