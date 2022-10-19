package kt.fluxo.core.debug

import kt.fluxo.core.internal.FluxoIntent
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

// FIXME: Only for test/debug variants?
internal actual fun <S, SE : Any> debugIntentInfo(mvvmIntent: FluxoIntent<S, SE>): FluxoIntent<S, SE>? {
    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    val traceElement = Throwable().stackTrace.getOrNull(2)
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
        for (field in clazz.declaredFields) {
            val fieldName = field.name
            val modifiers = field.modifiers
            if (modifiers and Modifier.STATIC != Modifier.STATIC && fieldName.startsWith('$')) {
                field.setAccessibleSafe()
                val name = fieldName.substring(1)
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
    return result
}

private fun AccessibleObject.setAccessibleSafe() {
    try {
        // TODO: No AccessibleObject.canAccess method on Android, but should use it for Java 9+?
        @Suppress("DEPRECATION")
        if (isAccessible) {
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
