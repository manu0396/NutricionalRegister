plugins {
    id("nutrition.kmp-library")
    id("nutrition.sqldelight")
    alias(libs.plugins.kover)
}

apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(libs.coroutines.core)
        implementation(libs.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines.extensions)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
        implementation(libs.sqldelight.android.driver)
    }

    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.sqldelight.native.driver)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.ktor.client.mock)
        implementation(libs.coroutines.test)
    }

    sourceSets.androidHostTest.dependencies {
        implementation(libs.sqldelight.sqlite.driver)
    }
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

tasks.named("koverVerify") {
    val measurableSources =
        fileTree("src") {
            include("**/commonMain/**/*.kt", "**/androidMain/**/*.kt", "**/jvmMain/**/*.kt")
        }
    onlyIf("core:data has JVM-measurable production sources") { measurableSources.files.isNotEmpty() }
}
