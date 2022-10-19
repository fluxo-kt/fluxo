package kt.fluxo.core

import kotlinx.coroutines.CompletableDeferred

public sealed interface StoreRequest<out Intent, out State> {

    /**
     * A request to forcibly set the [State] to a specific [value][state].
     */
    public class RestoreState<Intent, out State>(
        public val deferred: CompletableDeferred<Unit>?,
        public val state: State,
    ) : StoreRequest<Intent, State>

    /**
     * A request to handle an [intent].
     */
    public class HandleIntent<out Intent, State>(
        public val deferred: CompletableDeferred<Unit>?,
        public val intent: Intent,
    ) : StoreRequest<Intent, State>
}
