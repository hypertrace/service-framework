plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(projects.platformServiceFramework)
  api(platform(libs.grpc.bom))
  api(libs.grpc.api)
  api(libs.grpc.services)
  api(libs.hypertrace.grpc.client.utils)
  api(libs.typesafe.config)
  api(projects.serviceFrameworkSpi)

  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

  implementation(libs.slf4j.api)
  implementation(libs.hypertrace.grpc.server.utils)
}
