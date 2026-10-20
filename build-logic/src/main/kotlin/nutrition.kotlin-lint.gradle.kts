import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

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
        include("src/**/*.kt")
        exclude("**/build/**")
    }
}

// KtLint source-set tasks match include/exclude patterns relative to each source
// directory root, so generated-code roots under build/ can never be excluded with
// a path pattern (e.g. "**/build/**" never matches). Exclude generated sources by
// absolute path in every check task instead; this keeps SQLDelight-generated code
// out of lint while still checking all authored sources.
tasks.withType(KtLintCheckTask::class.java).configureEach {
    exclude(
        Spec<FileTreeElement> { element ->
            element.file.absolutePath
                .contains("${java.io.File.separator}build${java.io.File.separator}generated${java.io.File.separator}")
        },
    )
}
