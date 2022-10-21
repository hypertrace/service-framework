plugins {
  `java-library`
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(projects.platformServiceFramework)
  api(libs.hypertrace.grpc.client.utils)
  api(libs.typesafe.config)
  api(libs.javax.servlet.api)
  api(libs.google.guice)
  api(projects.serviceFrameworkSpi)

  implementation(libs.slf4j.api)
  implementation(libs.google.guice.servlet)
  implementation(libs.google.guava)
  implementation(libs.jetty.servlet)
  implementation(libs.jetty.server)
  implementation(libs.jetty.servlets)

  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

}
