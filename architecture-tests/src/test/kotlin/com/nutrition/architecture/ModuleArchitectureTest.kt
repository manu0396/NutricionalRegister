package com.nutrition.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test
import kotlin.test.assertTrue

/**
 * The architecture rules of the shared modules.
 *
 * The rules are enforced on the real source directories of the repository, not
 * on the module that hosts the test. The previous version of this file matched
 * a package root that no module uses, so it scanned nothing, found no
 * violation and could never fail. The guard at the top of every test makes
 * that class of mistake loud: a rule that inspects no file is a failure, not a
 * pass.
 */
class ModuleArchitectureTest {
    @Test
    fun `the domain does not depend on the data layer or on a feature`() {
        val files = sharedScope()

        val violations =
            files.flatMap { file ->
                val packageName = file.packageName()
                if (!packageName.startsWith(DOMAIN_PACKAGE)) {
                    return@flatMap emptyList()
                }
                file.imports
                    .filter { import -> import.name.startsWith(DATA_PACKAGE) || import.name.startsWith(FEATURE_PACKAGE) }
                    .map { import -> "${file.path}: ${import.name}" }
            }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `a feature does not depend on the data layer`() {
        val files = sharedScope()

        val violations =
            files.flatMap { file ->
                val packageName = file.packageName()
                if (!packageName.startsWith(FEATURE_PACKAGE)) {
                    return@flatMap emptyList()
                }
                file.imports
                    .filter { import -> import.name.startsWith(DATA_PACKAGE) }
                    .map { import -> "${file.path}: ${import.name}" }
            }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `the shared common code stays platform neutral`() {
        val files = sharedScope()

        val violations =
            files
                .filter { file -> file.sourceSetName == "commonMain" }
                .flatMap { file ->
                    file.imports
                        .filter { import -> import.name.startsWith("android.") || import.name.startsWith("java.") }
                        .map { import -> "${file.path}: ${import.name}" }
                }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    /**
     * Every Kotlin file of the shared modules.
     *
     * The scope is built from the real source directories, so a module that is
     * added has to be added here as well, and a rule that inspects no file
     * fails the build instead of passing it.
     */
    private fun sharedScope(): List<KoFileDeclaration> {
        val files = Konsist.scopeFromDirectories(SHARED_SOURCE_DIRECTORIES.toList()).files
        assertTrue(
            files.isNotEmpty(),
            "The architecture scope found no Kotlin file, so the rules above cannot fail",
        )
        return files
    }

    /** The package the file declares. */
    private fun KoFileDeclaration.packageName(): String =
        text
            .lineSequence()
            .firstOrNull { line -> line.startsWith("package ") }
            ?.removePrefix("package ")
            ?.trim()
            .orEmpty()

    private companion object {
        /** The real package root of the shared domain. */
        const val DOMAIN_PACKAGE = "com.example.nutricionalregister.domain"

        /** The real package root of the data layer. */
        const val DATA_PACKAGE = "com.example.nutricionalregister.data"

        /** The real package root of the features. */
        const val FEATURE_PACKAGE = "com.example.nutricionalregister.feature"

        /**
         * The source directories of the shared modules.
         *
         * The directories are named one by one, because a wildcard would also
         * walk the generated sources of the build directory.
         */
        val SHARED_SOURCE_DIRECTORIES =
            arrayOf(
                "core/domain/src",
                "core/data/src",
                "feature/assistant/src",
                "feature/diary/src",
                "feature/savedproducts/src",
                "feature/scan/src",
                "feature/search/src",
                "feature/summary/src",
                "shared/src",
            )
    }
}
