package org.hypertrace.core.serviceframework;

import java.util.ArrayList;
import java.util.List;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.IntegrationTestConfigClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service launcher that allows for starting multiple services by their respective names
 *
 * <p>The service names should be given as arguments. Ex: entity-service, attribute-service.
 *
 * <p>The launcher takes care of initializing the appropriate config client from its corresponding
 * resources/configs/application.conf
 */
public class IntegrationTestServiceLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformServiceLauncher.class);
  private static final List<PlatformService> PLATFORM_SERVICES = new ArrayList<>();

  /** @param serviceNames list of services to start */
  public static void main(String[] serviceNames) {
    for (String serviceName : serviceNames) {
      try {
        LOGGER.info("Trying to start PlatformService: {}", serviceName);
        final ConfigClient configClient =
            IntegrationTestConfigClientFactory.getConfigClientForService(serviceName);
        PlatformService app = PlatformServiceFactory.get(configClient);
        app.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
        PLATFORM_SERVICES.add(app);
        app.start();
      } catch (Exception e) {
        LOGGER.error("Got exception while starting PlatformService: " + serviceName, e);
      }
    }
  }

  public static void launchService(String testName, String serviceName) {
    try {
      LOGGER.info("Trying to start PlatformService: {}", serviceName);
      final ConfigClient configClient =
              IntegrationTestConfigClientFactory.getConfigClientForService(testName, serviceName);
      PlatformService app = PlatformServiceFactory.get(configClient);
      app.initialize();
      Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
      PLATFORM_SERVICES.add(app);
      app.start();
    } catch (Exception e) {
      LOGGER.error("Got exception while starting PlatformService: " + serviceName, e);
    }
  }

  static void shutdown() {
    PLATFORM_SERVICES.forEach(PlatformService::shutdown);
  }
}
