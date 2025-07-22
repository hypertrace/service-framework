plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  api(commonLibs.hypertrace.documentstore)
  api(projects.serviceFrameworkSpi)
  implementation(projects.platformMetrics)
  implementation(commonLibs.guava)
}
