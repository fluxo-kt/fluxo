gradlePlugin {
    plugins {
        create("release-dependencies-diff-create") {
            id = "release-dependencies-diff-create"
            implementationClass = "ReleaseDependenciesCreatePlugin"
        }
        create("release-dependencies-diff-compare") {
            id = "release-dependencies-diff-compare"
            implementationClass = "ReleaseDependenciesDiffPlugin"
        }
    }
}
