package kt.fluxo.core.internal

import kt.fluxo.core.Store
import kt.fluxo.core.annotation.InternalFluxoApi
import kotlin.jvm.Volatile

/**
 * A Guardian protects the integrity of the [Store] state against potential problems,
 * especially with race conditions due to parallel processing.
 */
@InternalFluxoApi
internal open class InputStrategyGuardian(
    private val parallelProcessing: Boolean = false,
) {
    @Volatile
    private var stateAccessed: Boolean = false

    @Volatile
    private var sideJobPosted: Boolean = false

    @Volatile
    private var usedProperly: Boolean = false

    @Volatile
    private var closed: Boolean = false

    open fun checkStateAccess() {
        if (parallelProcessing) {
            performStateAccessCheck()
        }
        checkNotClosed()
        checkNoSideJobs()
        stateAccessed = true
        usedProperly = true
    }

    open fun checkStateUpdate() {
        if (parallelProcessing) {
            performStateAccessCheck()
        }
        checkNotClosed()
        checkNoSideJobs()
        stateAccessed = true
        usedProperly = true
    }

    open fun checkPostSideEffect() {
        checkNotClosed()
        checkNoSideJobs()
        usedProperly = true
    }

    open fun checkNoOp() {
        checkNotClosed()
        checkNoSideJobs()
        usedProperly = true
    }

    open fun checkSideJob() {
        checkNotClosed()
        sideJobPosted = true
        usedProperly = true
    }

    open fun close() {
        checkNotClosed()
        checkUsedProperly()
        closed = true
    }


    private fun performStateAccessCheck() {
        check(!stateAccessed) {
            "ParallelInputStrategy requires that inputs only access or update the state at most once as a " +
                "safeguard against race conditions."
        }
    }

    private fun checkNotClosed() {
        check(!closed) { "This InputHandlerScope has already been closed" }
    }

    private fun checkNoSideJobs() {
        check(!sideJobPosted) { "SideJobs must be the last statements of the InputHandler" }
    }

    private fun checkUsedProperly() {
        check(usedProperly) {
            "Intent was not handled properly. To ensure you're following the MVI model properly, make sure any " +
                "sideJobs are executed in a `sideJob { }` block."
        }
    }
}
