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
  api(projects.serviceFrameworkSpi)
  implementation(projects.platformMetrics)

  api(libs.slf4j.api)
  api(libs.typesafe.config)

  // Use for thread dump servlet
  implementation(libs.dropwizard.metrics.servlets)
  implementation(libs.jetty.servlet)

  // Use for metrics servlet
  implementation(libs.prometheus.servlet)

  // http client
  implementation(libs.apache.httpclient)

  testImplementation(libs.apache.log4j.slf4jImpl)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.bundles.mockito)
  testImplementation(libs.jetty.servlet) {
    artifact {
      classifier = "tests"
    }
  }
  testImplementation(libs.jetty.http) {
    artifact {
      classifier = "tests"
    }
  }
}
