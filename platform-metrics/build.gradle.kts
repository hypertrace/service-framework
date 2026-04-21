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
  implementation(localLibs.micrometer.jvm.extras)
  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.dropwizard.metrics.jvm)
  implementation(localLibs.prometheus.simpleclient.dropwizard)
  implementation(localLibs.prometheus.simpleclient.servlet.jakarta)
  implementation(localLibs.prometheus.simpleclient.pushgateway)
  implementation(commonLibs.jetty.servlet)
  implementation(commonLibs.guava)

  compileOnly(localLibs.caffeine)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(localLibs.caffeine)
  testRuntimeOnly(commonLibs.log4j.slf4j2.impl)
}
