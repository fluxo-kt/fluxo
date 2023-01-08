package kt.fluxo.core.input

import kotlinx.coroutines.CompletableDeferred
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kotlin.jvm.JvmField

/**
 *
 * **NOTE: please fill an issue if you need this class to be open for your own specific implementations!**
 *
 * @see ChannelBasedInputStrategy for usage
 */
@ExperimentalFluxoApi
internal class IntentRequest<out Intent>(
    @JvmField val intent: Intent,
    @JvmField val deferred: CompletableDeferred<Unit>?,
)
