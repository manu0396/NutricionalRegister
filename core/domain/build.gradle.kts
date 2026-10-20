plugins {
    id("nutrition.kmp-library")
    alias(libs.plugins.kover)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.coroutines.core)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.coroutines.test)
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
    onlyIf("core:domain has JVM-measurable production sources") { measurableSources.files.isNotEmpty() }
}
