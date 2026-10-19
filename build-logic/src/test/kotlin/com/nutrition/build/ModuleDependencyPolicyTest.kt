package com.nutrition.build

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleDependencyPolicyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `allows declared feature dependency on domain`() {
        val projectDirectory =
            createFixture(
                """
                project(":feature:search") {
                    dependencies {
                        implementation(project(":core:domain"))
                    }
                }
                """.trimIndent(),
            )

        val result = runner(projectDirectory).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":moduleDependencyCheck")?.outcome)
    }

    @Test
    fun `rejects feature dependency on common`() {
        val projectDirectory =
            createFixture(
                """
                project(":feature:search") {
                    dependencies {
                        implementation(project(":core:common"))
                    }
                }
                """.trimIndent(),
            )

        val result = runner(projectDirectory).buildAndFail()

        assertTrue(
            result.output.contains(
                ":feature:search -> :core:common (implementation)",
            ),
            result.output,
        )
    }

    private fun createFixture(extraBuildScript: String): File {
        val projectDirectory = temporaryFolder.newFolder()
        File(projectDirectory, "settings.gradle").writeText(
            """
            rootProject.name = "dependency-policy-fixture"
            include(":core:common", ":core:domain", ":core:data", ":feature:search")
            """.trimIndent(),
        )
        listOf(
            "core/common",
            "core/domain",
            "core/data",
            "feature/search",
        ).forEach { File(projectDirectory, it).mkdirs() }
        File(projectDirectory, "build.gradle").writeText(
            """
            plugins {
                id "nutrition.module-dependency-policy"
            }
            allprojects {
                apply plugin: "java-library"
            }
            $extraBuildScript
            """.trimIndent(),
        )
        return projectDirectory
    }

    private fun runner(projectDirectory: File): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath()
            .withArguments("moduleDependencyCheck", "--stacktrace")
}
