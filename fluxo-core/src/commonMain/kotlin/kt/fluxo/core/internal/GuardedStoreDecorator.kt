package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kt.fluxo.core.SideJob
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.factory.StoreDecoratorBase
import kotlin.coroutines.CoroutineContext

internal class GuardedStoreDecorator<Intent, State, SideEffect : Any>(
    private val guardian: InputStrategyGuardian,
    store: StoreDecorator<Intent, State, SideEffect>,
) : StoreDecoratorBase<Intent, State, SideEffect>(store) {

    override var value: State
        get() {
            guardian.checkStateAccess()
            return super.value
        }
        set(value) {
            guardian.checkStateUpdate()
            super.value = value
        }

    override fun compareAndSet(expect: State, update: State): Boolean {
        guardian.checkStateUpdate()
        return super.compareAndSet(expect, update)
    }

    override fun send(intent: Intent): Job {
        guardian.checkEmitIntent()
        return super.send(intent)
    }

    override suspend fun emit(value: Intent) {
        guardian.checkEmitIntent()
        super.emit(value)
    }

    override suspend fun postSideEffect(sideEffect: SideEffect) {
        guardian.checkPostSideEffect()
        super.postSideEffect(sideEffect)
    }

    override suspend fun sideJob(
        key: String,
        context: CoroutineContext,
        start: CoroutineStart,
        block: SideJob<Intent, State, SideEffect>,
    ): Job {
        guardian.checkSideJob()
        return super.sideJob(key = key, context = context, start = start, block = block)
    }

    override fun noOp() {
        guardian.checkNoOp()
    }


    override fun close() {
        guardian.close()
        // Wrapped store shouldn't be closed here!
    }
}
