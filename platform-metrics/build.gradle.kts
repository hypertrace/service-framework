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
  api("io.dropwizard.metrics:metrics-core:4.1.0")
  api("io.micrometer:micrometer-core:1.5.3")
  api("org.apache.flink:flink-metrics-core:1.10.1")
  api("org.apache.flink:flink-metrics-prometheus_2.12:1.10.1")
  api("javax.servlet:javax.servlet-api:3.1.0")
  api("com.google.guava:guava:29.0-jre")

  implementation("io.micrometer:micrometer-registry-prometheus:1.5.3")
  implementation("io.github.mweirauch:micrometer-jvm-extras:0.2.0")
  implementation("org.slf4j:slf4j-api:1.7.25")
  implementation("io.dropwizard.metrics:metrics-jvm:4.1.0")
  implementation("io.prometheus:simpleclient_dropwizard:0.6.0")
  implementation("io.prometheus:simpleclient_servlet:0.6.0")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.18.v20190429")

  testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
  testImplementation("org.mockito:mockito-core:3.3.3")
}
