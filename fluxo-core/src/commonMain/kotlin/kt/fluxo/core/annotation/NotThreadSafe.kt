package kt.fluxo.core.annotation

/**
 * The class to which this annotation is applied is not thread-safe. This
 * annotation primarily exists for clarifying the non-thread-safety of a class
 * that might otherwise be assumed to be thread-safe, despite the fact that it
 * is a bad idea to assume a class is thread-safe without good reason.
 *
 * @see javax.annotation.concurrent.NotThreadSafe
 * @see ThreadSafe
 */
@MustBeDocumented
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Retention(AnnotationRetention.BINARY)
@Suppress("KDocUnresolvedReference")
@Target(AnnotationTarget.CLASS)
public expect annotation class NotThreadSafe()
