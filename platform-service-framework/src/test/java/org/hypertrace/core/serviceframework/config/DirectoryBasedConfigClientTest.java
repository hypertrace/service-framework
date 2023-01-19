package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import java.net.URL;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DirectoryBasedConfigClientTest {

  @Test
  public void testReadConfig() {
    URL configDirURL = this.getClass().getClassLoader().getResource("configs");
    final DirectoryBasedConfigClient configClient =
        new DirectoryBasedConfigClient(configDirURL.getPath());
    final Config config = configClient.getConfig("sample-app", "staging", "gcp01", null);
    Assertions.assertEquals("sample-app", config.getString("service.name"));
    Assertions.assertEquals(8099, config.getInt("service.admin.port"));
    Assertions.assertEquals(
        "org.hypertrace.core.example.SamplePlatformService", config.getString("main.class"));
    Assertions.assertEquals(
        Arrays.asList("stdout", "file", "kafka"), config.getStringList("logger.names"));
    Assertions.assertEquals("localhost:9092", config.getString("logger.kafka.brokers"));
    Assertions.assertEquals("sample-app-log", config.getString("logger.kafka.topic"));
    Assertions.assertEquals(
        Arrays.asList("console", "prometheus"), config.getStringList("metrics.reporter.names"));
    Assertions.assertEquals(
        "org.hypertrace.core.sample-app", config.getString("metrics.reporter.prefix"));
    Assertions.assertEquals(35, config.getInt("metrics.reportInterval"));
    Assertions.assertEquals("sample-app-staging-gcp01", config.getString("deployment.id"));
    Assertions.assertEquals("gcp01:5092", config.getString("service.discovery.url"));
  }

  @Test
  public void testConfigOverride() {
    URL configDirURL = this.getClass().getClassLoader().getResource("configs");
    final DirectoryBasedConfigClient configClient =
        new DirectoryBasedConfigClient(configDirURL.getPath());

    Config clusterConfig = configClient.getConfig("sample-app", "staging", null, null);
    Assertions.assertEquals("sample-app-staging", clusterConfig.getString("deployment.id"));
    Config podConfig1 = configClient.getConfig("sample-app", "staging", "gcp01", null);
    Assertions.assertEquals("sample-app-staging-gcp01", podConfig1.getString("deployment.id"));
    Assertions.assertEquals("gcp01:5092", podConfig1.getString("service.discovery.url"));
    Config podConfig2 = configClient.getConfig("sample-app", "staging", "gcp02", null);
    Assertions.assertEquals("sample-app-staging-gcp02", podConfig2.getString("deployment.id"));
    Assertions.assertEquals("gcp02:5092", podConfig2.getString("service.discovery.url"));
  }
}
