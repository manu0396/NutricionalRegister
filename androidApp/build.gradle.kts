plugins {
    id("nutrition.android-application")
}

android {
    namespace = "com.example.nutricionalregister"

    defaultConfig {
        applicationId = "com.example.nutricionalregister"
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
