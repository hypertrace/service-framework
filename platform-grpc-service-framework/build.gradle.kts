plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(projects.platformServiceFramework)
  api(commonLibs.grpc.api)
  api(localLibs.grpc.services)
  api(commonLibs.hypertrace.grpcutils.client)
  api(commonLibs.typesafe.config)
  api(commonLibs.protobuf.java)
  api(projects.serviceFrameworkSpi)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  implementation(projects.platformMetrics)
  implementation(commonLibs.grpc.inprocess)
  implementation(commonLibs.grpc.netty)
  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.hypertrace.grpcutils.server)
}
