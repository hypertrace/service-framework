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

  api("org.slf4j:slf4j-api:1.7.25")
  api("com.typesafe:config:1.3.2")

  // Use for thread dump servlet
  implementation("io.dropwizard.metrics:metrics-servlets:4.1.0")
  implementation("org.eclipse.jetty:jetty-servlet:9.4.18.v20190429")

  // http client
  implementation("org.apache.httpcomponents:httpclient:4.5.13")

  constraints {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.0") {
      because("Deserialization of Untrusted Data [High Severity][https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-561587] in com.fasterxml.jackson.core:jackson-databind@2.9.8\n" +
              "   io.dropwizard.metrics:metrics-servlets")
    }
    implementation("commons-codec:commons-codec:1.13") {
      because("version 1.12 has a vulnerability https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518")
    }
  }

  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")
  testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
  testImplementation("org.mockito:mockito-core:3.3.3")
  testImplementation("org.eclipse.jetty:jetty-servlet:9.4.18.v20190429:tests")
  testImplementation("org.eclipse.jetty:jetty-http:9.4.18.v20190429:tests")
}
