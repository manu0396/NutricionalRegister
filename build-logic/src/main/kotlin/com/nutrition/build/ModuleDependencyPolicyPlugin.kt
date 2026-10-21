package com.nutrition.build

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty

public class ModuleDependencyPolicyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "Apply nutrition.module-dependency-policy to the root project."
        }

        val violations: ListProperty<String> = project.objects.listProperty(String::class.java)

        project.tasks.register("moduleDependencyCheck") {
            group = "verification"
            description = "Checks project-module dependencies against the architecture allowlist."

            doLast {
                val found = violations.get()
                if (found.isNotEmpty()) {
                    throw GradleException(
                        buildString {
                            appendLine("Forbidden project dependencies:")
                            found.forEach { appendLine(" - $it") }
                        },
                    )
                }
            }
        }

        // Collect violations at configuration time so the task action captures only the
        // serializable ListProperty, keeping the task compatible with the configuration cache.
        project.gradle.projectsEvaluated {
            violations.set(collectViolations(project))
        }
    }

    private fun collectViolations(root: Project): List<String> =
        root.allprojects
            .flatMap { sourceProject ->
                sourceProject.configurations.flatMap { configuration ->
                    if (!configuration.isCanBeDeclared ||
                        configuration.name.contains("classpath", ignoreCase = true)
                    ) {
                        return@flatMap emptyList()
                    }

                    configuration.dependencies
                        .withType(ProjectDependency::class.java)
                        .mapNotNull { dependency ->
                            val targetPath = dependency.path
                            if (
                                targetPath != sourceProject.path &&
                                targetPath !in allowedDependencies(sourceProject.path)
                            ) {
                                "${sourceProject.path} -> $targetPath (${configuration.name})"
                            } else {
                                null
                            }
                        }
                }
            }.distinct()

    private fun allowedDependencies(projectPath: String): Set<String> =
        when {
            projectPath == ":core:domain" || projectPath == ":core:common" -> emptySet()
            projectPath == ":core:data" -> setOf(":core:domain")
            projectPath.startsWith(":feature:") -> setOf(":core:domain")
            projectPath == ":androidApp" ->
                setOf(
                    ":core:data",
                    ":feature:diary",
                    ":feature:scan",
                    ":feature:savedproducts",
                    ":feature:search",
                    ":feature:summary",
                )
            projectPath == ":shared" ->
                setOf(
                    ":core:data",
                    ":core:domain",
                    ":feature:diary",
                    ":feature:scan",
                    ":feature:savedproducts",
                    ":feature:search",
                    ":feature:summary",
                )
            projectPath == ":architecture-tests" -> emptySet()
            else -> emptySet()
        }
}
