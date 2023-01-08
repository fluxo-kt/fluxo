package kt.fluxo.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest

internal open class CoroutineScopeAwareTest(context: CoroutineContext) {
    internal companion object {
        internal const val INIT = "init"
    }

    constructor() : this(EmptyCoroutineContext)

    protected val scope = CoroutineScope(context)

    @AfterTest
    open fun afterTest() {
        scope.cancel()
    }
}
