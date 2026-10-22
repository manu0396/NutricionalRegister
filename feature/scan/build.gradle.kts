plugins {
    id("nutrition.kmp-library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:domain"))
        implementation(libs.coroutines.core)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.coroutines.test)
    }
}
