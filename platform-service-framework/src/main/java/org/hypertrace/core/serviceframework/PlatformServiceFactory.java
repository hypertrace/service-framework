package org.hypertrace.core.serviceframework;

import com.typesafe.config.Config;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class that instantiates the concrete PlatformService from the config file.
 *
 * <p>The main.class configuration property in the config file is used for this.
 */
public class PlatformServiceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformServiceFactory.class);

  @SuppressWarnings("unchecked")
  public static synchronized PlatformService get(ConfigClient configClient) {
    final Config config = configClient.getConfig();
    final String mainClass = config.getString("main.class");
    try {
      Class<PlatformService> serviceClass = (Class<PlatformService>) Class.forName(mainClass);
      return serviceClass.getDeclaredConstructor(ConfigClient.class).newInstance(configClient);
    } catch (Exception e) {
      LOGGER.error("PlatformService main class could not be found - main.class = {}", mainClass, e);
      System.err.println(
          String.format(
              "PlatformService main class could not be found - main.class = %s", mainClass));
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
