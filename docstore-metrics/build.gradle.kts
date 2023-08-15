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
  implementation(project(":platform-metrics"))
}
