plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

tasks.test {
  useJUnitPlatform()
}

dependencies {
  api(projects.serviceFrameworkSpi)
  implementation(projects.platformMetrics)

  api(commonLibs.slf4j2.api)
  implementation(localLibs.apache.httpcomponents.httpclient)

  // Use for thread dump servlet
  implementation(localLibs.dropwizard.metrics.jakarta.servlets)
  implementation(commonLibs.jetty.servlet)

  // Use for metrics servlet
  implementation(localLibs.prometheus.simpleclient.servlet.jakarta)

  testImplementation(commonLibs.log4j.slf4j2.impl)
  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
}
