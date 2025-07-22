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
  implementation(projects.platformServiceFramework)

  // Configuration
  implementation(commonLibs.typesafe.config)
  // Logging
  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.awaitility)
}
