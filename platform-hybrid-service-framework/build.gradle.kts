plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":platform-grpc-service-framework"))
  api(project(":platform-http-service-framework"))
  api(project(":platform-service-framework"))

  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
}
