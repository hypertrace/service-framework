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
    implementation("com.typesafe:config:1.3.2")
    // Logging
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.awaitility:awaitility:3.1.6")
}
