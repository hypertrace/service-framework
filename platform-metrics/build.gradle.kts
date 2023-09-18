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
  api("io.dropwizard.metrics:metrics-core:4.2.16")
  api("io.micrometer:micrometer-core:1.10.2")
  api("javax.servlet:javax.servlet-api:3.1.0")

  implementation("io.micrometer:micrometer-registry-prometheus:1.10.2")

  implementation("io.github.mweirauch:micrometer-jvm-extras:0.2.2")
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
  implementation("io.dropwizard.metrics:metrics-jvm:4.2.16")
  implementation("io.prometheus:simpleclient_dropwizard:0.12.0")
  implementation("io.prometheus:simpleclient_servlet:0.12.0")
  implementation("io.prometheus:simpleclient_pushgateway:0.12.0")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.52.v20230823")
  implementation("com.google.guava:guava:32.0.1-jre")

  testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
  testImplementation("org.mockito:mockito-core:4.8.0")
}
