plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

  implementation(project(":platform-metrics"))
  implementation(libs.hypertrace.documentStore)
}
