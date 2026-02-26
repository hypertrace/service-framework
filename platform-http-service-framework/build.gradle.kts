plugins {
  `java-library`
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.platformServiceFramework)
  api(commonLibs.hypertrace.grpcutils.client)
  api(commonLibs.typesafe.config)
  api(localLibs.jakarta.servlet.api)
  api(commonLibs.guice)
  api(projects.serviceFrameworkSpi)

  implementation(projects.platformMetrics)
  implementation(commonLibs.slf4j2.api)
  implementation(localLibs.guice.servlet)
  implementation(commonLibs.guava)
  implementation(commonLibs.jetty.servlet)
  implementation(commonLibs.jetty.server)
  implementation(commonLibs.jetty.servlets)
  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)
}
