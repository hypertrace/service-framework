plugins {
  `java-library`
  jacoco
  id("org.hypertrace.publish-plugin")
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":platform-service-framework"))
  api(platform("io.grpc:grpc-bom:1.68.3"))
  api("io.grpc:grpc-api")
  api("io.grpc:grpc-services")
  api("org.hypertrace.core.grpcutils:grpc-client-utils:0.13.14")
  api("com.typesafe:config:1.4.2")
  api("com.google.protobuf:protobuf-java:3.25.5")
  api(project(":service-framework-spi"))

  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

  implementation(project(":platform-metrics"))
  implementation("io.grpc:grpc-inprocess")
  implementation("io.grpc:grpc-netty")
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("org.hypertrace.core.grpcutils:grpc-server-utils:0.13.14")
}
