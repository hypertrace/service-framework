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
  api(commonLibs.typesafe.config)
  api(localLibs.dropwizard.metrics.jakarta.servlet)
  api(localLibs.micrometer.core)
  api(localLibs.jakarta.servlet.api)

  implementation(localLibs.micrometer.registry.prometheus.simpleclient)
  implementation(localLibs.prometheus.simpleclient.pushgateway)
  // Bridges Dropwizard MetricRegistry into the simpleclient CollectorRegistry.
  implementation(localLibs.prometheus.simpleclient.dropwizard)
  // Standalone HTTP server that exposes the Prometheus scrape endpoint (SharedMeterRegistry).
  implementation(localLibs.prometheus.simpleclient.httpserver)

  implementation(localLibs.micrometer.jvm.extras)
  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.dropwizard.metrics.jvm)
  implementation(commonLibs.jetty.servlet)
  implementation(commonLibs.guava)

  compileOnly(localLibs.caffeine)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(localLibs.caffeine)
  testRuntimeOnly(commonLibs.log4j.slf4j2.impl)
}
