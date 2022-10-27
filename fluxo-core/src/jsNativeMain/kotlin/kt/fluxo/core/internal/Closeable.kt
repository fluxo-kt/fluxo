package kt.fluxo.core.internal

import kotlin.js.JsName


public actual interface Closeable {
    @JsName("close")
    public actual fun close()
}
