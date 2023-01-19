package org.hypertrace.core.serviceframework.config;

import java.util.Optional;
import org.hypertrace.core.serviceframework.IntegrationTestServiceLauncher;

/**
 * Used by {@link IntegrationTestServiceLauncher} to initialize the config client for the respective
 * service Config is loaded from resources/config/application.conf
 */
public class IntegrationTestConfigClientFactory {
  public static ConfigClient getConfigClientForService(String serviceName) {
    return new IntegrationTestConfigClient(serviceName);
  }

  public static ConfigClient getConfigClientForService(
      Optional<String> testName, String serviceName) {
    return new IntegrationTestConfigClient(testName, serviceName);
  }
}
