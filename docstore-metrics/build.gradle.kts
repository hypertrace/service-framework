plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

  api(libs.hypertrace.documentStore)
  api(project(":service-framework-spi"))
  api(platform("com.fasterxml.jackson:jackson-bom:2.16.0"))
  implementation(project(":platform-metrics"))
  implementation("com.google.guava:guava:32.0.1-jre")
}
