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
  implementation(commonLibs.guice.servlet)
  implementation(commonLibs.guava)
  implementation(localLibs.jetty.ee10.servlet)
  implementation(localLibs.jetty.server)
  implementation(localLibs.jetty.ee10.servlets)
  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)
}
