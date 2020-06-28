package org.hypertrace.core.serviceframework.config;

import org.hypertrace.core.serviceframework.IntegrationTestServiceLauncher;

/**
 * Used by {@link IntegrationTestServiceLauncher} to initialize
 * the config client for the respective service
 * Config is loaded from resources/config/application.conf
 */
public class IntegrationTestConfigClientFactory {
  public static synchronized ConfigClient getConfigClientForService(String serviceName) {
    String resourcePrefix = "configs" + "/" + serviceName;
    return new IntegrationTestConfigClient(resourcePrefix);
  }
}
