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
  constraints {
    implementation(libs.jackson.databind) {
      because("""
        [https://nvd.nist.gov/vuln/detail/CVE-2022-42004] [https://nvd.nist.gov/vuln/detail/CVE-2022-42003]
          in 'com.fasterxml.jackson.core:jackson-databind:2.12.7' > 'io.dropwizard.metrics:metrics-servlets:4.2.10'
      """)
    }
  }
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
