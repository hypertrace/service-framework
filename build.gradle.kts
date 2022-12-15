import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  id("org.hypertrace.repository-plugin") version "0.4.0"
  id("org.hypertrace.ci-utils-plugin") version "0.3.0"
  id("org.hypertrace.publish-plugin") version "1.0.2" apply false
  id("org.hypertrace.jacoco-report-plugin") version "0.2.0" apply false
  id("org.hypertrace.code-style-plugin") version "1.1.2" apply false
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
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11

      apply(plugin = "org.hypertrace.code-style-plugin")
    }
  }
}
