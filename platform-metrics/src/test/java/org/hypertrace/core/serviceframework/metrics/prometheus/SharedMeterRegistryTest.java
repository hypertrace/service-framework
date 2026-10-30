package org.hypertrace.core.serviceframework.metrics.prometheus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedMeterRegistryTest {
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  @BeforeEach
  void startEndpoint() {
    SharedMeterRegistry.start("shared-meter-registry-test", 0);
  }

  @AfterEach
  void stopRegistry() {
    PlatformMetricsRegistry.stop();
  }

  @Test
  void servesPlatformAndRegisteredMetricsOnOneEndpoint() throws Exception {
    PlatformMetricsRegistry.registerCounter("shared.test.platform", Map.of()).increment();
    CollectorRegistry external = new CollectorRegistry();
    Counter.build().name("shared_test_external").help("help").register(external).inc();
    Collector handle = SharedMeterRegistry.register(external);

    try {
      String body = scrape(null).body();

      assertTrue(body.contains("shared_test_platform_total"), body);
      assertTrue(body.contains("shared_test_external_total"), body);
    } finally {
      SharedMeterRegistry.unregister(handle);
    }
  }

  @Test
  void unregisterRemovesRegisteredMetrics() throws Exception {
    CollectorRegistry external = new CollectorRegistry();
    Counter.build().name("shared_test_removed").help("help").register(external).inc();
    Collector handle = SharedMeterRegistry.register(external);

    SharedMeterRegistry.unregister(handle);

    assertFalse(scrape(null).body().contains("shared_test_removed"));
  }

  @Test
  void restartReinitializesPlatformMetricsOnBoundEndpoint() throws Exception {
    int port = SharedMeterRegistry.boundPort();
    PlatformMetricsRegistry.stop();

    SharedMeterRegistry.start("shared-meter-registry-test", 0);
    PlatformMetricsRegistry.registerCounter("shared.test.restarted", Map.of()).increment();

    assertEquals(port, SharedMeterRegistry.boundPort());
    assertTrue(scrape(null).body().contains("shared_test_restarted_total"));
  }

  @Test
  void servesDistributionWithPercentilesAndHistogram() throws Exception {
    PlatformMetricsRegistry.registerDistributionSummary("shared.test.summary", Map.of(), true)
        .record(1);

    HttpResponse<String> response = scrape(null);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("shared_test_summary_bucket"), response.body());
    assertTrue(response.body().contains("quantile="), response.body());
  }

  @Test
  void servesOpenMetricsFormat() throws Exception {
    PlatformMetricsRegistry.registerCounter("shared.test.openmetrics", Map.of()).increment();

    HttpResponse<String> response = scrape("application/openmetrics-text; version=1.0.0");

    assertEquals(200, response.statusCode());
    assertTrue(
        response.headers().firstValue("Content-Type").orElse("").contains("openmetrics-text"));
    assertTrue(response.body().contains("shared_test_openmetrics_total"), response.body());
  }

  private static HttpResponse<String> scrape(String accept)
      throws IOException, InterruptedException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(
            URI.create("http://localhost:" + SharedMeterRegistry.boundPort() + "/metrics"));
    if (accept != null) {
      request.header("Accept", accept);
    }
    return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
