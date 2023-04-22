package impl

internal object EnvParams {

    val metadataOnly: Boolean get() = System.getProperty("metadata_only") != null

    // FIXME: Windows build in the split_targets mode still prepares JS environment, android AARs, detekt analyzes JVM, etc.
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4084164094/jobs/7040541558#step:10:704
    val splitTargets: Boolean get() = System.getProperty("split_targets") != null
}
