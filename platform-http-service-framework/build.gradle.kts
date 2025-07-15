plugins {
  `java-library`
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":platform-service-framework"))
  api("org.hypertrace.core.grpcutils:grpc-client-utils:0.13.14")
  api("com.typesafe:config:1.4.2")
  api("jakarta.servlet:jakarta.servlet-api:6.0.0")
  api("com.google.inject:guice:7.0.0")
  api(project(":service-framework-spi"))

  implementation(project(":platform-metrics"))
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("com.google.inject.extensions:guice-servlet:7.0.0")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("org.eclipse.jetty:jetty-servlet:11.0.24")
  implementation("org.eclipse.jetty:jetty-server:11.0.24")
  implementation("org.eclipse.jetty:jetty-servlets:11.0.24")
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
}
