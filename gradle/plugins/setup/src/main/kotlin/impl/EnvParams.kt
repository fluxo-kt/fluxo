package impl

internal object EnvParams {

    val metadataOnly: Boolean get() = System.getProperty("metadata_only") != null

    val splitTargets: Boolean get() = System.getProperty("split_targets") != null
}
