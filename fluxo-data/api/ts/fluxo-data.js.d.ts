type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class FluxoResult<out T> /* implements Serializable */ {
    private constructor();
    get value(): T;
    get error(): Nullable<Error>;
    get isNotLoaded(): boolean;
    get isCached(): boolean;
    get isLoading(): boolean;
    get isEmpty(): boolean;
    get isSuccess(): boolean;
    get isFailure(): boolean;
    get isFailed(): boolean;
    toString(): string;
    copy(value?: T, error?: Nullable<Error>, flags?: number): FluxoResult<T>;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace FluxoResult {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <T>() => FluxoResult<T>;
    }
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                notLoaded(): FluxoResult<Nullable<never>>;
                notLoadedWithValue<T>(value: T): FluxoResult<T>;
                cached<T>(value: T): FluxoResult<T>;
                loading(): FluxoResult<Nullable<never>>;
                loadingWithValue<T>(value: T): FluxoResult<T>;
                empty(): FluxoResult<Nullable<never>>;
                emptyWithValue<T>(value: T): FluxoResult<T>;
                success<T>(value: T): FluxoResult<T>;
                failure(error: Nullable<Error>): FluxoResult<Nullable<never>>;
                failureWithValue<T>(error: Nullable<Error>, value: T): FluxoResult<T>;
                private constructor();
            }
        }
    }
}
export declare function resultOf<R>(block: () => R): FluxoResult<Nullable<R>>;
export declare function getOrThrow<T>(_this_: FluxoResult<T>): T;
export declare function getOrElse<R, T extends R>(_this_: FluxoResult<T>, onFailure: (exception: Nullable<Error>) => R): R;
export declare function getOrDefault<R, T extends R>(_this_: FluxoResult<Nullable<T>>, defaultValue: () => R): R;
export declare function fold<R, T>(_this_: FluxoResult<T>, onSuccess: (value: T) => R, onFailure: (exception: Nullable<Error>) => R): R;
export declare function isValid<T>(_this_: FluxoResult<T>, predicate: (p0: T) => boolean): boolean;
export declare function map<R, T extends R>(_this_: FluxoResult<T>, transform: (p0: T) => R): FluxoResult<R>;
export declare function mapCatching<R, T extends R>(_this_: FluxoResult<T>, transform: (value: T) => R): FluxoResult<Nullable<R>>;
export declare function recover<R, T extends R>(_this_: FluxoResult<T>, transform: (exception: Nullable<Error>) => R): FluxoResult<R>;
export declare function recoverCatching<R, T extends R>(_this_: FluxoResult<T>, transform: (exception: Nullable<Error>) => R): FluxoResult<Nullable<R>>;
export declare function cached<R, T extends R>(_this_: FluxoResult<T>, value?: R): FluxoResult<R>;
export declare function loading<R, T extends R>(_this_: FluxoResult<T>, value?: R): FluxoResult<R>;
export declare function success<R, T extends R>(_this_: FluxoResult<T>, value?: R): FluxoResult<R>;
export declare function failure<R, T extends R>(_this_: FluxoResult<T>, error?: Nullable<Error>, value?: R): FluxoResult<R>;
export declare function onSuccess<T>(_this_: FluxoResult<T>, action: (p0: T) => void): FluxoResult<T>;
export declare function onFailure<T>(_this_: FluxoResult<T>, action: (p0: FluxoResult<T>, p1: Nullable<Error>) => void): FluxoResult<T>;
