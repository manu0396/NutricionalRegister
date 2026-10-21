plugins {
    id("nutrition.module-dependency-policy")
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("architectureTest") {
    group = "verification"
    description = "Runs source architecture tests and the Gradle module dependency policy."
    dependsOn(":architecture-tests:test", "moduleDependencyCheck")
}

tasks.register("coverageVerify") {
    group = "verification"
    description = "Verifies JVM line coverage for populated core modules."
    dependsOn(":core:domain:koverVerify", ":core:data:koverVerify")
}

tasks.register("kotlinLint") {
    group = "verification"
    description = "Runs ktlint checks across project modules and build logic."
    dependsOn(subprojects.filter { it.childProjects.isEmpty() }.map { "${it.path}:ktlintCheck" })
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintSourceCheck"))
}

tasks.register<Exec>("plainTextCheck") {
    group = "verification"
    description = "Fails if repository files contain emoji (constitution Principle V)."
    commandLine("bash", "scripts/check-no-emoji.sh")
}