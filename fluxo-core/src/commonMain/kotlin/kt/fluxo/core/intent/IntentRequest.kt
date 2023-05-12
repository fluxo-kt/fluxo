package kt.fluxo.core.intent

import kotlinx.coroutines.CompletableDeferred
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kotlin.jvm.JvmField

/**
 *
 * **NOTE: please fill an issue if you need this class to be open for your own specific implementations!**
 *
 * @see ChannelBasedIntentStrategy for usage
 */
@ExperimentalFluxoApi
internal class IntentRequest<out Intent>(
    @JvmField val intent: Intent,
    @JvmField val deferred: CompletableDeferred<Unit>?,
)
