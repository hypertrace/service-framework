package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Config client used for integration tests to load the config from the {@link
 * #resourcePrefix}/application.conf file that's present in the classpath.
 */
public class IntegrationTestConfigClient implements ConfigClient {
  private static final String APPLICATION_CONFIG_FILE_SUFFIX = "/application.conf";
  private static final String CONFIGS_PREFIX = "configs/";
  private static final String INTEGRATION_TEST_COMMON_DIRECTORY = "common";
  private static final String INTEGRATION_TEST_CLUSTER = "local";

  private final String defaultServiceName;

  public IntegrationTestConfigClient(String defaultServiceName) {
    this.defaultServiceName = defaultServiceName;
  }

  @Override
  public Config getConfig() {
    return this.getConfig(this.defaultServiceName, INTEGRATION_TEST_CLUSTER, null, null);
  }

  @Override
  public Config getConfig(String service, String cluster, String pod, String container) {
    return loadConfig(service, cluster, pod, container)
        .withFallback(loadConfig(service, cluster, pod))
        .withFallback(loadConfig(service, cluster))
        .withFallback(loadConfig(service))
        .withFallback(loadConfig(INTEGRATION_TEST_COMMON_DIRECTORY));
  }

  private Config loadConfig(String... segments) {
    return ConfigFactory.parseResources(
        Arrays.stream(segments)
            .collect(Collectors.joining("/", CONFIGS_PREFIX, APPLICATION_CONFIG_FILE_SUFFIX)));
  }
}
