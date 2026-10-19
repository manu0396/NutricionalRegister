import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("nutrition.kotlin-lint")
}

val libsCatalog =
    project.extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

android {
    compileSdk =
        libsCatalog
            .findVersion("android-compileSdk")
            .get()
            .requiredVersion
            .toInt()

    defaultConfig {
        minSdk =
            libsCatalog
                .findVersion("android-minSdk")
                .get()
                .requiredVersion
                .toInt()
        targetSdk =
            libsCatalog
                .findVersion("android-targetSdk")
                .get()
                .requiredVersion
                .toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}
