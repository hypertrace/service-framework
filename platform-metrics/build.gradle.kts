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
  api(libs.typesafe.config)
  api(libs.dropwizard.metrics.core)
  api(libs.micrometer.core)
  api(libs.javax.servlet.api)

  implementation(libs.micrometer.registry.prometheus)

  implementation(libs.micrometer.jvm.extras)
  implementation(libs.slf4j.api)
  implementation(libs.dropwizard.metrics.jvm)
  implementation(libs.prometheus.dropwizard)
  implementation(libs.prometheus.servlet)
  implementation(libs.prometheus.pushgateway)
  implementation(libs.jetty.servlet)
  implementation (libs.google.guava)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.bundles.mockito)
}
