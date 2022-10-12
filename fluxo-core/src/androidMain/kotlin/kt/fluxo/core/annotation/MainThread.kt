package kt.fluxo.core.annotation


/**
 * Denotes that the annotated method should only be called on the main thread.
 * If the annotated element is a class, then all methods in the class should be called
 * on the main thread.
 *
 * **Note:** Ordinarily, an app's main thread is also the UI
 * thread. However, under special circumstances, an app's main thread
 * might not be its UI thread.
 *
 * @see androidx.annotation.MainThread
 * @see androidx.annotation.UiThread
 */
public actual typealias MainThread = androidx.annotation.MainThread
