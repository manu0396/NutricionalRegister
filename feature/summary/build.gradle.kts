plugins {
    id("nutrition.kmp-library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
    }
}
