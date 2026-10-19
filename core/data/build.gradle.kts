plugins {
    id("nutrition.kmp-library")
    alias(libs.plugins.kover)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
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
    onlyIf("core:data has JVM-measurable production sources") {
        fileTree("src") {
            include("**/commonMain/**/*.kt", "**/androidMain/**/*.kt", "**/jvmMain/**/*.kt")
        }.files.isNotEmpty()
    }
}
