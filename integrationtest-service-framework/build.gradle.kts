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
    implementation(project(":platform-service-framework"))

    // Configuration
    implementation("com.typesafe:config:1.4.2")
    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.awaitility:awaitility:4.0.3")
}
