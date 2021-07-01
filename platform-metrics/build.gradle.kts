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
  api("com.typesafe:config:1.4.1")
  api("io.dropwizard.metrics:metrics-core:4.1.16")
  api("io.micrometer:micrometer-core:1.5.3")
  api("javax.servlet:javax.servlet-api:3.1.0")

  implementation("io.micrometer:micrometer-registry-prometheus:1.5.3")
  implementation("io.github.mweirauch:micrometer-jvm-extras:0.2.0")
  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("io.dropwizard.metrics:metrics-jvm:4.1.16")
  implementation("io.prometheus:simpleclient_dropwizard:0.6.0")
  implementation("io.prometheus:simpleclient_servlet:0.6.0")
  implementation("io.prometheus:simpleclient_pushgateway:0.9.0")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.39.v20210325")
  implementation ("com.google.guava:guava:30.1.1-jre")


  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
  testImplementation("org.mockito:mockito-core:3.8.0")
}
