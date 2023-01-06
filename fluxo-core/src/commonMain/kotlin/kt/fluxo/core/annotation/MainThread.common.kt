@file:Suppress("MatchingDeclarationName")

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
@MustBeDocumented
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Retention(AnnotationRetention.BINARY)
@Suppress("KDocUnresolvedReference", "GrazieInspection")
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
public expect annotation class MainThread()
