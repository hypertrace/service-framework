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
  implementation(project(":platform-metrics"))

  api("org.slf4j:slf4j-api:1.7.30")
  api("com.typesafe:config:1.4.1")

  // Use for thread dump servlet
  implementation("io.dropwizard.metrics:metrics-servlets:4.1.16")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.44.v20210927")

  // Use for metrics servlet
  implementation("io.prometheus:simpleclient_servlet:0.12.0")

  // http client
  implementation("org.apache.httpcomponents:httpclient:4.5.13")

  constraints {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2") {
      because("Multiple vulnerabilities")
    }
    implementation("commons-codec:commons-codec:1.15") {
      because("version 1.12 has a vulnerability https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518")
    }
  }

  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
  testImplementation("org.mockito:mockito-core:3.8.0")
  testImplementation("org.eclipse.jetty:jetty-servlet:9.4.44.v20210927:tests")
  testImplementation("org.eclipse.jetty:jetty-http:9.4.44.v20210927:tests")
}
