package kt.fluxo.core.debug

import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntent
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

// TODO: Only for test/debug variants?
internal actual fun <I> debugIntentWrapper(intent: I): I? {
    @Suppress("UNCHECKED_CAST") val mvvmIntent = (intent as? FluxoIntent<Any?, Any>) ?: return null

    @Suppress("ThrowingExceptionsWithoutMessageOrCause") val traceElement = Throwable().stackTrace.getOrNull(2)
    val methodName = traceElement?.methodName.let {
        when {
            !it.isNullOrEmpty() && it != "invoke" && it != "invokeSuspend" -> it
            else -> traceElement?.className?.substringAfterLast('.')
        }
    }

    // Kotlin stores values captured by lambda in `$<name>` fields, let's get values
    val clazz = mvvmIntent.javaClass
    val cached = intentArgumentsCache[clazz]
    val arguments: List<Pair<String, Any?>> = if (cached != null) {
        cached.map { (name, field) -> name to field[mvvmIntent] }
    } else {
        val args = ArrayList<Pair<String, Any?>>()
        val cache = ArrayList<Pair<String, Field>>()

        // Expected that fluxoIntent object will always have some fields
        fields@ for (field in clazz.declaredFields) {
            val fieldName = field.name
            val modifiers = field.modifiers
            if (modifiers and Modifier.STATIC != Modifier.STATIC) {
                val startIndex = when {
                    fieldName.startsWith('$') -> 1
                    fieldName.startsWith("arg$") -> 4
                    else -> continue@fields
                }
                field.setAccessibleSafe()
                val name = fieldName.substring(startIndex)
                cache.add(name to field)
                args.add(name to field[mvvmIntent])
            }
        }

        if (cache.isEmpty()) {
            intentArgumentsCache[clazz] = EMPTY_ARGUMENTS_CACHE
            emptyList()
        } else {
            intentArgumentsCache[clazz] = cache.toTypedArray()
            args.trimToSize()
            args
        }
    }

    val result = FluxoIntentDebug(methodName, arguments, mvvmIntent)
    print("$result")
    @Suppress("UNCHECKED_CAST") return result as I
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

private fun AccessibleObject.setAccessibleSafe() {
    try {
        // TODO: No AccessibleObject.canAccess method on Android, but should use it for Java 9+?
        @Suppress("DEPRECATION") if (isAccessible) {
            return
        }
        isAccessible = true
    } catch (_: RuntimeException) {
        // setAccessible not allowed by security policy
        // InaccessibleObjectException: Unable to make .. accessible (JDK9+)
    }
}

private val intentArgumentsCache = ConcurrentHashMap<Class<*>, Array<Pair<String, Field>>>()
private val EMPTY_ARGUMENTS_CACHE = emptyArray<Pair<String, Field>>()
