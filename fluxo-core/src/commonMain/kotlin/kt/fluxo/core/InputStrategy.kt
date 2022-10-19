package kt.fluxo.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.dsl.InputStrategyScope
import kotlin.jvm.Volatile

public abstract class InputStrategy<Intent, State> {

    public open fun createQueue(): Channel<StoreRequest<Intent, State>> {
        return Channel(Channel.BUFFERED, BufferOverflow.SUSPEND)
    }

    public open val rollbackOnCancellation: Boolean get() = true

    public abstract suspend fun InputStrategyScope<Intent, State>.processInputs(filteredQueue: Flow<StoreRequest<Intent, State>>)


    /**
     * A Guardian protects the integrity of the [StoreMvvm] state against potential problems,
     * especially with race conditions due to parallel processing.
     */
    // FIXME: Use ony when debug checks enabled
    @ExperimentalFluxoApi
    public open class Guardian(
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

        public open fun checkStateAccess() {
            if (parallelProcessing) {
                performStateAccessCheck()
            }
            checkNotClosed()
            checkNoSideJobs()
            stateAccessed = true
            usedProperly = true
        }

        public open fun checkStateUpdate() {
            if (parallelProcessing) {
                performStateAccessCheck()
            }
            checkNotClosed()
            checkNoSideJobs()
            stateAccessed = true
            usedProperly = true
        }

        public open fun checkPostSideEffect() {
            checkNotClosed()
            checkNoSideJobs()
            usedProperly = true
        }

        public open fun checkNoOp() {
            checkNotClosed()
            checkNoSideJobs()
            usedProperly = true
        }

        public open fun checkSideJob() {
            checkNotClosed()
            sideJobPosted = true
            usedProperly = true
        }

        public open fun close() {
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
}
