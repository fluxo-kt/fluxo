type Nullable<T> = T | null | undefined
export declare namespace kt.fluxo.core {
    interface FluxoException {
        readonly __doNotUseOrImplementIt: {
            readonly "kt.fluxo.core.FluxoException": unique symbol;
        };
    }
    class FluxoClosedException /* extends kotlin.coroutines.cancellation.CancellationException */ implements kt.fluxo.core.FluxoException {
        private constructor();
        readonly __doNotUseOrImplementIt: kt.fluxo.core.FluxoException["__doNotUseOrImplementIt"];
    }
    class FluxoRuntimeException /* extends kotlin.RuntimeException */ implements kt.fluxo.core.FluxoException {
        private constructor();
        readonly __doNotUseOrImplementIt: kt.fluxo.core.FluxoException["__doNotUseOrImplementIt"];
    }
}
export declare namespace kt.fluxo.core {
    class FluxoSettings<Intent, State, SideEffect extends any> {
        private constructor();
        get name(): Nullable<string>;
        set name(value: Nullable<string>);
        get lazy(): boolean;
        set lazy(value: boolean);
        get closeOnExceptions(): boolean;
        set closeOnExceptions(value: boolean);
        get debugChecks(): boolean;
        set debugChecks(value: boolean);
        get sideEffectBufferSize(): number;
        set sideEffectBufferSize(value: number);
        get bootstrapper(): Nullable<any /*Suspend functions are not supported*/>;
        set bootstrapper(value: Nullable<any /*Suspend functions are not supported*/>);
        onStart(bootstrapper: any /*Suspend functions are not supported*/): void;
        onCreate(bootstrapper: any /*Suspend functions are not supported*/): void;
        bootstrapperJob(key?: string, context?: any/* kotlin.coroutines.CoroutineContext */, start?: any/* kotlinx.coroutines.CoroutineStart */, onError?: Nullable<(p0: Error) => void>, block: any /*Suspend functions are not supported*/): void;
        get intentFilter(): Nullable<(p0: State, p1: Intent) => boolean>;
        set intentFilter(value: Nullable<(p0: State, p1: Intent) => boolean>);
        get intentStrategy(): any/* kt.fluxo.core.intent.IntentStrategy.Factory */;
        set intentStrategy(value: any/* kt.fluxo.core.intent.IntentStrategy.Factory */);
        get sideEffectStrategy(): any/* kt.fluxo.core.SideEffectStrategy */;
        set sideEffectStrategy(value: any/* kt.fluxo.core.SideEffectStrategy */);
        get scope(): Nullable<any>/* Nullable<kotlinx.coroutines.CoroutineScope> */;
        set scope(value: Nullable<any>/* Nullable<kotlinx.coroutines.CoroutineScope> */);
        get coroutineContext(): any/* kotlin.coroutines.CoroutineContext */;
        set coroutineContext(value: any/* kotlin.coroutines.CoroutineContext */);
        get sideJobsContext(): any/* kotlin.coroutines.CoroutineContext */;
        set sideJobsContext(value: any/* kotlin.coroutines.CoroutineContext */);
        get optimized(): boolean;
        set optimized(value: boolean);
        get exceptionHandler(): Nullable<any>/* Nullable<kotlinx.coroutines.CoroutineExceptionHandler> */;
        set exceptionHandler(value: Nullable<any>/* Nullable<kotlinx.coroutines.CoroutineExceptionHandler> */);
        setExceptionHandler(handler: (p0: any/* kotlin.coroutines.CoroutineContext */, p1: Error) => void): void;
        onError(handler: (p0: any/* kotlin.coroutines.CoroutineContext */, p1: Error) => void): void;
        get Fifo(): any/* kt.fluxo.core.intent.IntentStrategy.Factory */;
        get Lifo(): any/* kt.fluxo.core.intent.IntentStrategy.Factory */;
        get Parallel(): any/* kt.fluxo.core.intent.IntentStrategy.Factory */;
        get Direct(): any/* kt.fluxo.core.intent.IntentStrategy.Factory */;
        get repeatOnSubscribedStopTimeout(): any/* kotlin.Long */;
        set repeatOnSubscribedStopTimeout(value: any/* kotlin.Long */);
        copy(): kt.fluxo.core.FluxoSettings<Intent, State, SideEffect>;
        static get Factory(): {
            get DEFAULT(): kt.fluxo.core.FluxoSettings<Nullable<any>, Nullable<any>, any>;
            create<Intent, State, SideEffect extends any>(): kt.fluxo.core.FluxoSettings<Intent, State, SideEffect>;
        };
    }
}
export declare namespace kt.fluxo.core.dsl {
    interface ContainerHost<State, SideEffect extends any> {
        readonly container: any/* kt.fluxo.core.StoreSE<Suspend functions are not supported, State, SideEffect> */;
        readonly __doNotUseOrImplementIt: {
            readonly "kt.fluxo.core.dsl.ContainerHost": unique symbol;
        };
    }
}
export as namespace fluxo_fluxo_core;