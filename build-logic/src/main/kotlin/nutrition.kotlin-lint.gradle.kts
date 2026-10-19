import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

val libsCatalog =
    project.extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

ktlint {
    version.set(libsCatalog.findVersion("ktlintCli").get().requiredVersion)

    filter {
        include("src/**")
    }
}
