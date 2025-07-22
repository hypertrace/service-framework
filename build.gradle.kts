import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  alias(commonLibs.plugins.hypertrace.repository)
  alias(commonLibs.plugins.hypertrace.ciutils)
  alias(commonLibs.plugins.hypertrace.codestyle) apply false
  alias(commonLibs.plugins.hypertrace.publish) apply false
  alias(localLibs.plugins.hypertrace.java.convention)
  alias(commonLibs.plugins.owasp.dependencycheck)
}

subprojects {
  group = "org.hypertrace.core.serviceframework"
  pluginManager.withPlugin(rootProject.commonLibs.plugins.hypertrace.publish.get().pluginId) {
    configure<HypertracePublishExtension> {
      license.set(License.APACHE_2_0)
    }
  }
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      apply(plugin = rootProject.commonLibs.plugins.hypertrace.codestyle.get().pluginId)
    }
  }
}

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
  suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 3.0F
}