@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.debug

import kotlin.internal.InlineOnly

/** No native implementation */
@InlineOnly
internal actual inline fun <I> debugIntentWrapper(intent: I): I = intent
