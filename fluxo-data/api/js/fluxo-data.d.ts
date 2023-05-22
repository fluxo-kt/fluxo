type Nullable<T> = T | null | undefined
export declare namespace kt.fluxo.data {
    class FluxoResult<T> /* implements kotlin.io.Serializable */ {
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
        copy(value?: T, error?: Nullable<Error>, flags?: number): kt.fluxo.data.FluxoResult<T>;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
        static get Companion(): {
            notLoaded(): kt.fluxo.data.FluxoResult<Nullable<never>>;
            notLoadedWithValue<T>(value: T): kt.fluxo.data.FluxoResult<T>;
            cached<T>(value: T): kt.fluxo.data.FluxoResult<T>;
            loading(): kt.fluxo.data.FluxoResult<Nullable<never>>;
            loadingWithValue<T>(value: T): kt.fluxo.data.FluxoResult<T>;
            empty(): kt.fluxo.data.FluxoResult<Nullable<never>>;
            emptyWithValue<T>(value: T): kt.fluxo.data.FluxoResult<T>;
            success<T>(value: T): kt.fluxo.data.FluxoResult<T>;
            failure(error: Nullable<Error>): kt.fluxo.data.FluxoResult<Nullable<never>>;
            failureWithValue<T>(error: Nullable<Error>, value: T): kt.fluxo.data.FluxoResult<T>;
        };
    }
}
export declare namespace kt.fluxo.data {
    function resultOf<R>(block: () => R): kt.fluxo.data.FluxoResult<Nullable<R>>;
    function getOrThrow<T>(_this_: kt.fluxo.data.FluxoResult<T>): T;
    function getOrElse<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, onFailure: (p0: Nullable<Error>) => R): R;
    function getOrDefault<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<Nullable<T>>, defaultValue: () => R): R;
    function fold<R, T>(_this_: kt.fluxo.data.FluxoResult<T>, onSuccess: (p0: T) => R, onFailure: (p0: Nullable<Error>) => R): R;
    function isValid<T>(_this_: kt.fluxo.data.FluxoResult<T>, predicate: (p0: T) => boolean): boolean;
    function map<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, transform: (p0: T) => R): kt.fluxo.data.FluxoResult<R>;
    function mapCatching<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, transform: (p0: T) => R): kt.fluxo.data.FluxoResult<Nullable<R>>;
    function recover<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, transform: (p0: Nullable<Error>) => R): kt.fluxo.data.FluxoResult<R>;
    function recoverCatching<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, transform: (p0: Nullable<Error>) => R): kt.fluxo.data.FluxoResult<Nullable<R>>;
    function cached<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, value?: R): kt.fluxo.data.FluxoResult<R>;
    function loading<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, value?: R): kt.fluxo.data.FluxoResult<R>;
    function success<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, value?: R): kt.fluxo.data.FluxoResult<R>;
    function failure<R, T extends R>(_this_: kt.fluxo.data.FluxoResult<T>, error?: Nullable<Error>, value?: R): kt.fluxo.data.FluxoResult<R>;
    function onSuccess<T>(_this_: kt.fluxo.data.FluxoResult<T>, action: (p0: T) => void): kt.fluxo.data.FluxoResult<T>;
    function onFailure<T>(_this_: kt.fluxo.data.FluxoResult<T>, action: (p0: kt.fluxo.data.FluxoResult<T>, p1: Nullable<Error>) => void): kt.fluxo.data.FluxoResult<T>;
}
export as namespace fluxo_fluxo_data;
