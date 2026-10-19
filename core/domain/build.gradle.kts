plugins {
    id("nutrition.kmp-library")
    alias(libs.plugins.kover)
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
    onlyIf("core:domain has JVM-measurable production sources") {
        fileTree("src") {
            include("**/commonMain/**/*.kt", "**/androidMain/**/*.kt", "**/jvmMain/**/*.kt")
        }.files.isNotEmpty()
    }
}
