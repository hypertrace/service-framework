rootProject.name = "service-framework"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

include(":platform-service-framework")
include(":platform-metrics")
include(":integrationtest-service-framework")
include(":service-framework-spi")
