import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("nutrition.kotlin-lint")
}

val libsCatalog =
    project.extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.nutrition.${project.name.replace('-', '.')}"
        compileSdk =
            libsCatalog
                .findVersion("android-compileSdk")
                .get()
                .requiredVersion
                .toInt()
        minSdk =
            libsCatalog
                .findVersion("android-minSdk")
                .get()
                .requiredVersion
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
