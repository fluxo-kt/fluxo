@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER", "TooManyFunctions")

package kt.fluxo.data

import kotlin.internal.InlineOnly

/**
 * A wrapper type designed to represent the result of data fetching from any source or some calculations.
 * When data is refreshed, the previous values can be kept around for a nicer end-user experience.
 *
 * [FluxoData] can represent state in a mixed condition, like *"cached empty value"* or *"loading with some default data"*, etc.
 *
 * [Emptiness][isEmpty] detected automatically for [Collection], [CharSequence], and `null` values.
 *
 * [FluxoData] is recommended as a state for [Store][kt.fluxo.core.Store] if it suits your use-case.
 *
 * @see kotlin.Result
 * @see com.copperleaf.ballast.repository.cache.Cached
 * @see io.uniflow.core.flow.data.UIState
 */
@Suppress("SerialVersionUIDInSerializableClass", "KDocUnresolvedReference", "MemberVisibilityCanBePrivate")
public data class FluxoData<out T> internal constructor(
    public val value: T,
    public val error: Throwable?,
    private val flags: Int,
) : Serializable {

    val isNotLoaded: Boolean get() = flags.hasFlag(FLAG_NOT_LOADED)
    val isCached: Boolean get() = flags.hasFlag(FLAG_CACHED)
    val isLoading: Boolean get() = flags.hasFlag(FLAG_LOADING)
    val isEmpty: Boolean get() = flags.hasFlag(FLAG_EMPTY)
    val isSuccess: Boolean get() = flags.hasFlag(FLAG_SUCCESS)
    val isFailed: Boolean get() = flags.hasFlag(FLAG_FAILED) || error != null

    public companion object {
        /**
         * Not loaded data state, no value.
         */
        public fun notLoaded(): FluxoData<Nothing?> = NOT_LOADED

        /**
         * Not loaded data state with default [value].
         */
        public fun <T> notLoaded(value: T): FluxoData<T> = FluxoData(value, error = null, flags = FLAG_NOT_LOADED or isEmpty(value))


        /**
         * Successful data state, [value] filled from cache.
         */
        public fun <T> cached(value: T): FluxoData<T> =
            FluxoData(value, error = null, flags = FLAG_SUCCESS or FLAG_CACHED or isEmpty(value))


        /**
         * Loading data state, no previous value.
         */
        public fun loading(): FluxoData<Nothing?> = LOADING

        /**
         * Loading data state with previous [value]. Considers as successful if [value] is not empty!
         */
        public fun <T> loading(value: T): FluxoData<T> {
            val e = isEmpty(value)
            val s = if (e.hasFlag(FLAG_EMPTY)) 0 else FLAG_SUCCESS
            return FluxoData(value, error = null, flags = FLAG_LOADING or e or s)
        }


        /**
         * Successful, but empty data state.
         */
        public fun empty(): FluxoData<Nothing?> = EMPTY

        /**
         * Successful, but empty data state. [value] set explicitly.
         */
        public fun <T> empty(value: T): FluxoData<T> = FluxoData(value, error = null, flags = FLAG_SUCCESS or FLAG_EMPTY)


        /**
         * Successful data state with [value] set.
         */
        public fun <T> success(value: T): FluxoData<T> = FluxoData(value, error = null, FLAG_SUCCESS or isEmpty(value))


        /**
         * Failed state.
         */
        public fun failure(error: Throwable?): FluxoData<Nothing?> = FluxoData(value = null, error, flags = FLAG_FAILED or FLAG_EMPTY)

        /**
         * Failed state with [value] set explicitly.
         */
        public fun <T> failure(error: Throwable?, value: T): FluxoData<T> = FluxoData(value, error, flags = FLAG_FAILED or isEmpty(value))


        private const val FLAG_NOT_LOADED = 0x1
        private const val FLAG_CACHED = 0x2
        private const val FLAG_LOADING = 0x4
        private const val FLAG_EMPTY = 0x8
        private const val FLAG_SUCCESS = 0x100
        private const val FLAG_FAILED = 0x10000000

        private val NOT_LOADED = FluxoData(value = null, error = null, flags = FLAG_NOT_LOADED or FLAG_EMPTY)
        private val EMPTY = FluxoData(value = null, error = null, flags = FLAG_SUCCESS or FLAG_EMPTY)
        private val LOADING = FluxoData(value = null, error = null, flags = FLAG_LOADING or FLAG_EMPTY)

        @InlineOnly
        internal inline fun Int.hasFlag(flag: Int) = flag and this == flag
        private fun isEmpty(value: Any?): Int {
            @Suppress("ComplexCondition")
            return if (value == null || value is Collection<*> && value.isEmpty() || value is CharSequence && value.isEmpty()) {
                FLAG_EMPTY
            } else {
                0
            }
        }
    }

    public override fun toString(): String {
        val sb = StringBuilder(@Suppress("MagicNumber") 128)
        sb.append(
            when {
                isFailed -> "Failed"
                isNotLoaded -> "NotLoaded"
                isCached -> "Cached"
                isLoading -> "Loading"
                isEmpty -> "Empty"
                else -> "Success"
            },
        )
        var parentheses = false
        if (value != null) {
            parentheses = true
            sb.append('(')
            sb.append(value)
            if (error != null) sb.append(',').append(' ')
        }
        if (error != null) {
            if (!parentheses) {
                parentheses = true
                sb.append('(')
            }
            sb.append(error)
        }
        if (parentheses) {
            sb.append(')')
        }
        return sb.toString()
    }
}
