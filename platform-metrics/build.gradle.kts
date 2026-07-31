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

  // New Prometheus client stack (io.micrometer.prometheusmetrics / io.prometheus.metrics).
  implementation(localLibs.micrometer.registry.prometheus)
  // Standalone HTTP server that exposes the Prometheus scrape endpoint (SharedMeterRegistry).
  implementation(localLibs.prometheus.exporter.httpserver)
  // Bridges Dropwizard MetricRegistry into the new PrometheusRegistry.
  implementation(localLibs.prometheus.instrumentation.dropwizard)

  // Legacy simpleclient stack — still required by PrometheusPushMeterRegistry
  // (io.micrometer.prometheus.PrometheusMeterRegistry + PushGateway).
  implementation(localLibs.micrometer.registry.prometheus.simpleclient)
  implementation(localLibs.prometheus.simpleclient.pushgateway)

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
