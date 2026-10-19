import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("nutrition.kmp-library")
}

kotlin {
    val xcf = XCFramework("Shared")

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
            export(project(":core:data"))
            export(project(":core:domain"))
            export(project(":feature:diary"))
            export(project(":feature:scan"))
            export(project(":feature:search"))
            export(project(":feature:summary"))
            transitiveExport = true
        }
    }

    sourceSets.commonMain.dependencies {
        api(project(":core:data"))
        api(project(":core:domain"))
        api(project(":feature:diary"))
        api(project(":feature:scan"))
        api(project(":feature:search"))
        api(project(":feature:summary"))
    }
}
