package kt.fluxo.core.internal

import kt.fluxo.core.annotation.CallSuper

public expect interface Closeable {
    @CallSuper
    public fun close()
}
