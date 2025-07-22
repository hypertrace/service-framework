plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.publish)
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(projects.platformGrpcServiceFramework)
  api(projects.platformHttpServiceFramework)
  api(projects.platformServiceFramework)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)
}
