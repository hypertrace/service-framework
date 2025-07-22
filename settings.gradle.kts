import org.hypertrace.gradle.dependency.DependencyPluginSettingExtension

rootProject.name = "service-framework"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://us-maven.pkg.dev/hypertrace-repos/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.3.0"
  id("org.hypertrace.dependency-settings") version "0.2.0"
}

configure<DependencyPluginSettingExtension> {
  catalogVersion.set("0.3.51")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":platform-grpc-service-framework")
include(":platform-http-service-framework")
include(":platform-hybrid-service-framework")
include(":platform-service-framework")
include(":platform-metrics")
include(":docstore-metrics")
include(":integrationtest-service-framework")
include(":service-framework-spi")
