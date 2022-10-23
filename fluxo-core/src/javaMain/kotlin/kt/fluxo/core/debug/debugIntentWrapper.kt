package kt.fluxo.core.debug

import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntent
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

// TODO: Only for test/debug variants?
internal actual fun <I> debugIntentWrapper(intent: I): I? {
    @Suppress("UNCHECKED_CAST")
    val fluxoIntent = (intent as? FluxoIntent<Any?, Any>) ?: return null

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    val traceElement = Throwable().stackTrace.getOrNull(2)
    val methodName = traceElement?.methodName.let {
        when {
            !it.isNullOrEmpty() && it != "invoke" && it != "invokeSuspend" -> it // function name
            else -> traceElement?.className?.substringAfterLast('.') // class name
        }
    }

    // Kotlin stores values captured by lambda in `$<name>` fields, let's get values
    val clazz = fluxoIntent.javaClass
    val arguments: List<Pair<String, Any?>> = intentArgumentsCache[clazz]
        ?.map { (name, field) -> name to field[fluxoIntent] }
        ?: reflectArgs(clazz, fluxoIntent)

    @Suppress("UNCHECKED_CAST")
    return FluxoIntentDebug(methodName, arguments, fluxoIntent) as I
}

private fun reflectArgs(clazz: Class<FluxoIntent<Any?, Any>>, fluxoIntent: FluxoIntent<Any?, Any>): List<Pair<String, Any?>> {
    val args = ArrayList<Pair<String, Any?>>()
    val cache = ArrayList<Pair<String, Field>>()

    // Expected that fluxoIntent object will always have some fields
    fields@ for (field in clazz.declaredFields) {
        val fieldName = field.name
        val modifiers = field.modifiers
        if (modifiers and Modifier.STATIC != Modifier.STATIC) {
            @Suppress("MagicNumber")
            val startIndex = when {
                // For kotlin class lambdas (`-Xlambdas=class`/`-Xsam-conversions=class` Kotlin options)
                // funName(argName=value, argName=value, argName=value)
                fieldName.startsWith('$') -> 1
                /*
                 * For lambdas generated with `invokedynamic` (`-Xlambdas=indy`/`-Xsam-conversions=indy` Kotlin options)
                 * funName(1=value, 2=value, 3=value)
                 * See: https://kotlinlang.org/docs/whatsnew15.html#lambdas-via-invokedynamic
                 */
                fieldName.startsWith("arg$") -> 4
                else -> continue@fields
            }
            field.setAccessibleSafe()
            val name = fieldName.substring(startIndex)
            cache.add(name to field)
            args.add(name to field[fluxoIntent])
        }
    }

    if (cache.isEmpty()) {
        intentArgumentsCache[clazz] = EMPTY_ARGUMENTS_CACHE
        return emptyList()
    }

    intentArgumentsCache[clazz] = cache.toTypedArray()
    args.trimToSize()
    return args
}

private fun AccessibleObject.setAccessibleSafe() {
    try {
        // TODO: No AccessibleObject.canAccess method on Android, but should we use it for Java 9+?
        @Suppress("DEPRECATION") if (isAccessible) {
            return
        }
        isAccessible = true
    } catch (_: RuntimeException) {
        // setAccessible not allowed by security policy
        // InaccessibleObjectException: Unable to make .. accessible (JDK9+)
    }
}

private data class FluxoIntentDebug<S, SE : Any>(
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

private val intentArgumentsCache = ConcurrentHashMap<Class<*>, Array<Pair<String, Field>>>()
private val EMPTY_ARGUMENTS_CACHE = emptyArray<Pair<String, Field>>()
