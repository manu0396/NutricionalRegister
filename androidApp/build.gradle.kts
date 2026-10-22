plugins {
    id("nutrition.android-application")
}

/**
 * The address of the first-party AI gateway the Coach talks to.
 *
 * The address is build configuration and never a credential: the gateway
 * authenticates the app with a device attestation it collects itself, so no
 * key of any kind is shipped in the APK (ADR-0016). A build without the
 * property has no assistant, and the Coach screen says so plainly instead of
 * offering an input field that cannot work.
 */
val aiGatewayBaseUrl: String =
    providers.gradleProperty("NUTRILOG_AI_PROXY_URL").getOrElse("").trim()

android {
    namespace = "com.example.nutricionalregister"

    buildFeatures {
        // The address of the AI gateway is injected as a generated string
        // resource, because it is build configuration and not a credential.
        resValues = true
    }

    defaultConfig {
        applicationId = "com.example.nutricionalregister"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue(
            "string",
            "ai_gateway_base_url",
            aiGatewayBaseUrl.replace("&", "&amp;").replace("\"", "\\\""),
        )
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":feature:assistant"))
    implementation(project(":feature:diary"))
    implementation(project(":feature:savedproducts"))
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
    implementation(libs.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    androidTestImplementation(libs.compose.ui.test.junit4.android)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coil.test)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.uiTooling)
}
