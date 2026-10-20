plugins {
    id("nutrition.android-application")
}

android {
    namespace = "com.example.nutricionalregister"

    defaultConfig {
        applicationId = "com.example.nutricionalregister"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":feature:diary"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:search"))
    implementation(project(":feature:summary"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    androidTestImplementation(libs.compose.ui.test.junit4.android)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coil.test)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.uiTooling)
}
