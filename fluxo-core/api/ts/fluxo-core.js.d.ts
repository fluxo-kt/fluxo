type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface FluxoException {
    readonly __doNotUseOrImplementIt: {
        readonly "kt.fluxo.core.FluxoException": unique symbol;
    };
}
export declare class FluxoClosedException extends /* CancellationException */ Error implements FluxoException {
    private constructor();
    readonly __doNotUseOrImplementIt: FluxoException["__doNotUseOrImplementIt"];
}
export declare namespace FluxoClosedException {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => FluxoClosedException;
    }
}
export declare class FluxoRuntimeException extends /* RuntimeException */ Error implements FluxoException {
    private constructor();
    readonly __doNotUseOrImplementIt: FluxoException["__doNotUseOrImplementIt"];
}
export declare namespace FluxoRuntimeException {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => FluxoRuntimeException;
    }
}
export declare class FluxoSettings<Intent, State, SideEffect extends any> {
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
    /** @deprecated Use onStart instead */
    onCreate(bootstrapper: any /*Suspend functions are not supported*/): void;
    bootstrapperJob(key: string | undefined, context: any/* CoroutineContext */ | undefined, start: any/* CoroutineStart */ | undefined, onError: Nullable<(error: Error) => void> | undefined, block: any /*Suspend functions are not supported*/): void;
    get intentFilter(): Nullable<(p0: State, intent: Intent) => boolean>;
    set intentFilter(value: Nullable<(p0: State, intent: Intent) => boolean>);
    get intentStrategy(): any/* IntentStrategy.Factory */;
    set intentStrategy(value: any/* IntentStrategy.Factory */);
    get sideEffectStrategy(): any/* SideEffectStrategy */;
    set sideEffectStrategy(value: any/* SideEffectStrategy */);
    get scope(): Nullable<any>/* Nullable<CoroutineScope> */;
    set scope(value: Nullable<any>/* Nullable<CoroutineScope> */);
    get coroutineContext(): any/* CoroutineContext */;
    set coroutineContext(value: any/* CoroutineContext */);
    get sideJobsContext(): any/* CoroutineContext */;
    set sideJobsContext(value: any/* CoroutineContext */);
    get optimized(): boolean;
    set optimized(value: boolean);
    get exceptionHandler(): Nullable<any>/* Nullable<CoroutineExceptionHandler> */;
    set exceptionHandler(value: Nullable<any>/* Nullable<CoroutineExceptionHandler> */);
    setExceptionHandler(handler: (p0: any/* CoroutineContext */, p1: Error) => void): void;
    onError(handler: (p0: any/* CoroutineContext */, p1: Error) => void): void;
    get Fifo(): any/* IntentStrategy.Factory */;
    get Lifo(): any/* IntentStrategy.Factory */;
    get Parallel(): any/* IntentStrategy.Factory */;
    get Direct(): any/* IntentStrategy.Factory */;
    copy(): FluxoSettings<Intent, State, SideEffect>;
}
export declare namespace FluxoSettings {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <Intent, State, SideEffect extends any>() => FluxoSettings<Intent, State, SideEffect>;
    }
    abstract class Factory extends KtSingleton<Factory.$metadata$.constructor>() {
        private constructor();
    }
    namespace Factory {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                get DEFAULT(): FluxoSettings<Nullable<any>, Nullable<any>, any>;
                create<Intent, State, SideEffect extends any>(): FluxoSettings<Intent, State, SideEffect>;
                private constructor();
            }
        }
    }
}
export declare interface ContainerHost<State, SideEffect extends any> {
    readonly container: any/* StoreSE<Suspend functions are not supported, State, SideEffect> */;
    readonly __doNotUseOrImplementIt: {
        readonly "kt.fluxo.core.dsl.ContainerHost": unique symbol;
    };
}
