plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("moduleDependencyPolicy") {
            id = "nutrition.module-dependency-policy"
            implementationClass = "com.nutrition.build.ModuleDependencyPolicyPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

val ktlintCli by configurations.creating

dependencies {
    add("ktlintCli", "com.pinterest.ktlint:ktlint-cli:${libs.versions.ktlintCli.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:${libs.versions.ktlintGradle.get()}")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

val lintSourceFiles =
    files(
        fileTree("src") {
            include("**/*.kt", "**/*.gradle.kts")
        },
        "build.gradle.kts",
        "settings.gradle.kts",
    )

tasks.register<JavaExec>("ktlintSourceCheck") {
    group = "verification"
    description = "Checks authored build-logic Kotlin sources and scripts with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args(lintSourceFiles.files.map { it.absolutePath })
    inputs.files(lintSourceFiles)
}
