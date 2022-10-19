package kt.fluxo.core.debug

import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntent

@InternalFluxoApi
internal data class FluxoIntentDebug<S, SE : Any>(
    val methodName: String?,
    val arguments: List<Pair<String, Any?>>,
    val fluxoIntent: FluxoIntent<S, SE>,
) : (StoreScope<*, S, SE>) -> Unit by fluxoIntent {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(if (methodName.isNullOrEmpty()) "<unknown>" else methodName)
        if (arguments.isNotEmpty()) {
            arguments.joinTo(sb, separator = ", ", prefix = "(", postfix = ")") { (n, v) -> "$n=$v" }
        }
        return sb.toString()
    }
}
