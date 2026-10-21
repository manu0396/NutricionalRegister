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
        implementation(libs.billing.ktx)
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
        filters {
            // The Android Play Billing client is a thin bridge over the live Billing
            // SDK: every branch needs a real store connection and a foreground
            // Activity, so it cannot be exercised from a host test. Its only pure
            // logic (the BillingResult -> PurchaseFailure mapping) is unit-tested in
            // AndroidPurchaseClientTest; the integration itself is verified against
            // a Play test environment and recorded in ADR-0014. Excluding it keeps
            // the coverage gate measuring the shared, testable behaviour.
            excludes {
                classes(
                    "com.example.nutricionalregister.data.AndroidPurchaseClient",
                    "com.example.nutricionalregister.data.AndroidPurchaseClient$*",
                )
            }
        }
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
