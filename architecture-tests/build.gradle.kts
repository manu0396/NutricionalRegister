plugins {
    alias(libs.plugins.kotlinJvm)
    id("nutrition.kotlin-lint")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.konsist)
}

tasks.test {
    useJUnit()
}
