package org.hypertrace.core.serviceframework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a disk based directory hierarchy config client.
 *
 * <p>The default config directory should be put into [working_directory]/configs, config file name
 * is application.conf.
 *
 * <p>There is also common config directory which contains all the common configs across all the
 * services.
 *
 * <p>For each level of the service configs, it will first load common config at that level then
 * override it through the service config.
 *
 * <p>The configs will override based on the ordering from container to service. It's fine to skip
 * files at any level.
 *
 * <p>e.g. The override convention for application.conf is: ---- /path/to/configs/application.conf
 * ---- /path/to/configs/[service]/application.conf ----
 * /path/to/configs/[service]/[cluster]/application.conf ----
 * /path/to/configs/[service]/[cluster]/[pod]/application.conf ----
 * /path/to/configs/[service]/[cluster]/[pod]/[container]/application.conf
 *
 * <p>Common configs are sitting below: ---- /path/to/configs/common/application.conf ----
 * /path/to/configs/common/[cluster]/application.conf ----
 * /path/to/configs/common/[cluster]/[pod]/application.conf ----
 * /path/to/configs/common/[cluster]/[pod]/[container]/application.conf
 */
public class DirectoryBasedConfigClient implements ConfigClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryBasedConfigClient.class);
  private static final String APPLICATION_CONFIG_FILE = "application.conf";
  private static final String SERVICE_NAME = "service.name";
  private static final String CLUSTER_NAME = "cluster.name";
  private static final String POD_NAME = "pod.name";
  private static final String CONTAINER_NAME = "container.name";
  private static final String COMMON_DIRECTORY = "common";

  private final String baseDir;

  public DirectoryBasedConfigClient(String baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public Config getConfig() {
    return getConfig(
        ConfigUtils.getEnvironmentProperty(SERVICE_NAME),
        ConfigUtils.getEnvironmentProperty(CLUSTER_NAME),
        ConfigUtils.getEnvironmentProperty(POD_NAME),
        ConfigUtils.getEnvironmentProperty(CONTAINER_NAME));
  }

  @Override
  public Config getConfig(String service, String cluster, String pod, String container) {
    final Config unresolvedConfig = loadUnresolvedConfig(service, cluster, pod, container);
    LOGGER.info("Overrided Configs are listed below:");
    ConfigUtils.logConfFile(unresolvedConfig);
    return unresolvedConfig.resolve();
  }

  private Config loadUnresolvedConfig(
      String service, String cluster, String pod, String container) {
    LOGGER.info("Trying to compile configs under directory: {}", baseDir);
    Config serviceLevelConf =
        getConfigFromPath(String.format("%s/%s", baseDir, service))
            .withFallback(getConfigFromPath(String.format("%s/%s", baseDir, COMMON_DIRECTORY)));
    Config clusterLevelConf =
        getConfigFromPath(String.format("%s/%s/%s", baseDir, service, cluster))
            .withFallback(
                getConfigFromPath(String.format("%s/%s/%s", baseDir, COMMON_DIRECTORY, cluster)));
    final Config podLevelConf =
        getConfigFromPath(String.format("%s/%s/%s/%s", baseDir, service, cluster, pod))
            .withFallback(
                getConfigFromPath(
                    String.format("%s/%s/%s/%s", baseDir, COMMON_DIRECTORY, cluster, pod)));
    final Config containerLevelConf =
        getConfigFromPath(
                String.format("%s/%s/%s/%s/%s", baseDir, service, cluster, pod, container))
            .withFallback(
                getConfigFromPath(
                    String.format(
                        "%s/%s/%s/%s/%s", baseDir, COMMON_DIRECTORY, cluster, pod, container)));
    return containerLevelConf
        .withFallback(podLevelConf)
        .withFallback(clusterLevelConf)
        .withFallback(serviceLevelConf);
  }

  private Config getConfigFromPath(String configDir) {
    File serviceConfigFile = new File(configDir, APPLICATION_CONFIG_FILE);
    if (serviceConfigFile.exists()) {
      LOGGER.info("Loading config from path: {}", configDir + "/" + APPLICATION_CONFIG_FILE);
      return ConfigFactory.parseFile(serviceConfigFile);
    }
    return ConfigFactory.empty();
  }
}
