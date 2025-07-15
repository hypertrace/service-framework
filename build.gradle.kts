import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  id("org.hypertrace.repository-plugin") version "0.5.0"
  id("org.hypertrace.ci-utils-plugin") version "0.4.0"
  id("org.hypertrace.publish-plugin") version "1.1.1" apply false
  id("org.hypertrace.jacoco-report-plugin") version "0.3.0" apply false
  id("org.hypertrace.code-style-plugin") version "2.1.2" apply false
  id("org.hypertrace.java-convention") version "0.4.0"
  id("org.owasp.dependencycheck") version "12.1.0"
}

subprojects {
  group = "org.hypertrace.core.serviceframework"
  pluginManager.withPlugin("org.hypertrace.publish-plugin") {
    configure<HypertracePublishExtension> {
      license.set(License.APACHE_2_0)
    }
  }
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      apply(plugin = "org.hypertrace.code-style-plugin")
    }
  }
}

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
  suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 3.0F
}