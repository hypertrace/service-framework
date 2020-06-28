package org.hypertrace.core.serviceframework;

import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.IntegrationTestConfigClientFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

/**
 * Utility class used in Integration tests with helper methods to start/shutdown services
 */
public class IntegrationTestServerUtil {
  private static final long INITIAL_DELAY_IN_MILLIS = 1000L;
  private static final long INTERVAL_CHECK_IN_MILLIS = 1000L;
  private static final long MAX_CHECK_DURATION_IN_MILLIS = 60000L;

  private static ExecutorService executorService;
  private static String[] services;

  public static void startServices(String[] services) {
    executorService = Executors.newFixedThreadPool(services.length);
    IntegrationTestServerUtil.services = services;
    for (String service:services) {
      executorService.submit(() -> {
        try {
          IntegrationTestServiceLauncher.main(new String[] {service});
        } catch (Throwable t) {
          System.out.println("Error starting the service with these message: "
              + t.getMessage());
        }
      });
      waitTillServerReady(service);
    }
  }

  public static void shutdownServices() {
    for (String service:services) {
      IntegrationTestServiceLauncher.shutdown();
      Awaitility.await().pollInterval(INTERVAL_CHECK_IN_MILLIS, TimeUnit.MILLISECONDS)
          .and().pollDelay(INITIAL_DELAY_IN_MILLIS, TimeUnit.MILLISECONDS)
          .atMost(MAX_CHECK_DURATION_IN_MILLIS, TimeUnit.MILLISECONDS)
          .until(() -> !isServerReady(service));
    }
    executorService.shutdownNow();
  }

  private static void waitTillServerReady(String service) {
    Awaitility.await().pollInterval(INTERVAL_CHECK_IN_MILLIS, TimeUnit.MILLISECONDS)
        .and().pollDelay(INITIAL_DELAY_IN_MILLIS, TimeUnit.MILLISECONDS)
        .atMost(MAX_CHECK_DURATION_IN_MILLIS, TimeUnit.MILLISECONDS)
        .until(() -> isServerReady(service));
  }

  private static boolean isServerReady(String service) {
    try {
      ConfigClient configClient = IntegrationTestConfigClientFactory.getConfigClientForService(service);
      int port = configClient.getConfig().getInt("service.admin.port");
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(
              String.format("http://localhost:%d/health", port)))
          .build();

      HttpResponse<String> response =
          client.send(request, BodyHandlers.ofString());
      String responseBody = response.body();
      return response.statusCode() == 200 &&  "OK".equals(responseBody);
    } catch (IOException re) {
      return false;
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    return false;
  }

}
