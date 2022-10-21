plugins {
    `java-library`
    jacoco
    id("org.hypertrace.publish-plugin")
    id("org.hypertrace.jacoco-report-plugin")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.platformServiceFramework)

    // Configuration
    implementation(libs.typesafe.config)
    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.awaitility)
}
