rootProject.name = "service-framework"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://hypertrace.jfrog.io/artifactory/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.2.0"
}

include(":platform-service-framework")
include(":platform-metrics")
include(":integrationtest-service-framework")
include(":service-framework-spi")
