package org.hypertrace.core.serviceframework.config;

import org.hypertrace.core.serviceframework.IntegrationTestServiceLauncher;

import java.util.Optional;

/**
 * Used by {@link IntegrationTestServiceLauncher} to initialize
 * the config client for the respective service
 * Config is loaded from resources/config/application.conf
 */
public class IntegrationTestConfigClientFactory {
  public static ConfigClient getConfigClientForService(String serviceName) {
    return new IntegrationTestConfigClient(serviceName);
  }

  public static ConfigClient getConfigClientForService(Optional<String> testName, String serviceName) {
    return new IntegrationTestConfigClient(testName, serviceName);
  }
}