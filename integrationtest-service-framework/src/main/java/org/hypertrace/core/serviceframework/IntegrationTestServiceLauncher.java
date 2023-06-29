package org.hypertrace.core.serviceframework;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Stream;
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
  private static final Queue<PlatformService> PLATFORM_SERVICES = new LinkedList<>();

  public static void launchService(Optional<String> testName, String serviceName) {
    try {
      LOGGER.info("Trying to start PlatformService: {}", serviceName);
      final ConfigClient configClient =
          IntegrationTestConfigClientFactory.getConfigClientForService(testName, serviceName);
      PlatformService app = PlatformServiceFactory.get(configClient);
      app.initialize();
      PLATFORM_SERVICES.add(app);
      app.start();
    } catch (Exception e) {
      LOGGER.error("Got exception while starting PlatformService: " + serviceName, e);
    }
  }

  static void shutdown() {
    Stream.generate(PLATFORM_SERVICES::poll)
        .takeWhile(Objects::nonNull)
        .forEach(PlatformService::shutdown);
  }
}
