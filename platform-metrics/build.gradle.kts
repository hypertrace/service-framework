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
  api("com.typesafe:config:1.4.2")
  api("io.dropwizard.metrics:metrics-jakarta-servlet:4.2.25")
  api("io.micrometer:micrometer-core:1.14.4")
  api("jakarta.servlet:jakarta.servlet-api:6.0.0")

  // Using simpleclient flavour since with version >= 1.13.0 micrometer does not support io.prometheus.simpleclient dependencies
  // https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide
  implementation("io.micrometer:micrometer-registry-prometheus-simpleclient:1.14.4")

  implementation("io.github.mweirauch:micrometer-jvm-extras:0.2.2")
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("io.dropwizard.metrics:metrics-jvm:4.2.16")
  implementation("io.prometheus:simpleclient_dropwizard:0.16.0")
  implementation("io.prometheus:simpleclient_servlet_jakarta:0.16.0")
  implementation("io.prometheus:simpleclient_pushgateway:0.16.0")
  implementation("org.eclipse.jetty:jetty-servlet:11.0.24")
  implementation("com.google.guava:guava:32.0.1-jre")

  compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")

  testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
  testImplementation("org.mockito:mockito-core:4.8.0")
  testImplementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
  testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
}
