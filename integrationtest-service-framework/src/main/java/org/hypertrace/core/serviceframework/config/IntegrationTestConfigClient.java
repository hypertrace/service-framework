package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Config client used for integration tests to load the config from the
 * {@link #resourcePrefix}/application.conf file that's present in the classpath.
 */
public class IntegrationTestConfigClient implements ConfigClient {
  private static final String APPLICATION_CONFIG_FILE = "application.conf";
  private static final String INTEGRATION_TEST_ENV = "local";

  private String resourcePrefix;

  public IntegrationTestConfigClient(String resourcePrefix) {
    this.resourcePrefix = resourcePrefix;
  }

  @Override
  public Config getConfig() {
    Config config = ConfigFactory.parseResources(resourcePrefix + "/" + APPLICATION_CONFIG_FILE);
    Config localConfig = ConfigFactory.parseResources(resourcePrefix + "/" + INTEGRATION_TEST_ENV
        + "/" + APPLICATION_CONFIG_FILE);
    return localConfig.withFallback(config).resolve();
  }

  @Override
  public Config getConfig(String service, String cluster, String pod, String container) {
    throw new UnsupportedOperationException(
        "Loads from configs/application.conf. "
            + "Doesn't support different hierarchy of configurations");
  }
}
