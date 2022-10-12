@file:Suppress("KDocUnresolvedReference")
@file:OptIn(ExperimentalMultiplatform::class)

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
@OptionalExpectation
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
)
public expect annotation class MainThread()
