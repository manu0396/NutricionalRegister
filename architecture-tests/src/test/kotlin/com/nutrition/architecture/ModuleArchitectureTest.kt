package com.nutrition.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test
import kotlin.test.assertTrue

class ModuleArchitectureTest {
    @Test
    fun `domain is independent and data and features depend only on domain`() {
        val violations =
            Konsist.scopeFromProject().files.flatMap { file ->
                val packageName =
                    file.text
                        .lineSequence()
                        .firstOrNull { it.startsWith("package ") }
                        ?.removePrefix("package ")
                        ?.trim()
                        .orEmpty()
                val forbiddenImports =
                    when {
                        packageName.startsWith("com.nutrition.core.domain") ->
                            listOf("com.nutrition.core.data", "com.nutrition.feature")
                        packageName.startsWith("com.nutrition.core.data") ||
                            packageName.startsWith("com.nutrition.feature") ->
                            listOf("com.nutrition.core.data", "com.nutrition.feature")
                        else -> emptyList()
                    }

                file.imports
                    .filter { import -> forbiddenImports.any(import.name::startsWith) }
                    .map { import -> "${file.path}: ${import.name}" }
            }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }
}
