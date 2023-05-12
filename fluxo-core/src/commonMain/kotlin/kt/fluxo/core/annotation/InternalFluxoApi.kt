package kt.fluxo.core.annotation

/** @see kt.fluxo.common.annotation.InternalFluxoApi */
@Deprecated(
    message = "This is experimental Fluxo API. It may be changed in the future without notice.",
    replaceWith = ReplaceWith("kt.fluxo.common.annotation.ExperimentalFluxoApi"),
)
@Suppress("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN")
public typealias InternalFluxoApi = kt.fluxo.common.annotation.InternalFluxoApi
