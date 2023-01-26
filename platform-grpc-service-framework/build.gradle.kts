plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":platform-service-framework"))
  api(platform("io.grpc:grpc-bom:1.52.1"))
  api("io.grpc:grpc-api")
  api("io.grpc:grpc-services")
  api("org.hypertrace.core.grpcutils:grpc-client-utils:0.11.2")
  api("com.typesafe:config:1.4.2")
  api(project(":service-framework-spi"))

  annotationProcessor("org.projectlombok:lombok:1.18.24")
  compileOnly("org.projectlombok:lombok:1.18.24")

  implementation(project(":platform-metrics"))
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("org.hypertrace.core.grpcutils:grpc-server-utils:0.11.2")
}
