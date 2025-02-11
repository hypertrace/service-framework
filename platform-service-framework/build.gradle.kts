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
  api(project(":service-framework-spi"))
  api(platform("com.fasterxml.jackson:jackson-bom:2.16.0"))
  implementation(project(":platform-metrics"))

  api("org.slf4j:slf4j-api:1.7.36")
  api("com.typesafe:config:1.4.2")

  // Use for thread dump servlet
  implementation("io.dropwizard.metrics:metrics-jakarta-servlets:4.2.25")
  implementation("org.eclipse.jetty:jetty-servlet:11.0.24")

  // Use for metrics servlet
  implementation("io.prometheus:simpleclient_servlet_jakarta:0.16.0")

  // http client
  implementation("org.apache.httpcomponents:httpclient:4.5.13")

  constraints {
    implementation("commons-codec:commons-codec:1.15") {
      because("version 1.12 has a vulnerability https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518")
    }
  }

  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
  testImplementation("org.mockito:mockito-core:4.8.0")
}
