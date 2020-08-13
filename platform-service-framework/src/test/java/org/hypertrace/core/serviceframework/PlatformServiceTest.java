package org.hypertrace.core.serviceframework;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hypertrace.core.serviceframework.PlatformService.State;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlatformService}
 */
public class PlatformServiceTest {

  /**
   * A test service implementation.
   */
  static class TestService extends PlatformService {
    private final String name;

    public TestService(String name, ConfigClient client) {
      super(client);
      this.name = name;
    }

    @Override
    protected void doInit() {

    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
      PlatformMetricsRegistry.stop();
    }

    @Override
    public boolean healthCheck() {
      return true;
    }

    @Override
    public String getServiceName() {
      return this.name;
    }
  }

  private PlatformService getService(String name, Map<String, String> configs) {
    return new TestService(name, new ConfigClient() {
      @Override
      public Config getConfig() {
        return ConfigFactory.parseMap(configs);
      }

      @Override
      public Config getConfig(String service, String cluster, String pod, String container) {
        return null;
      }
    });
  }

  private void startService(final PlatformService service) {
    new Thread(() -> {
      service.initialize();
      service.start();
    }).start();

    // Wait for the service to fully start so that admin server is started and all servlets
    // are initialized.
    while (service.getServiceState() != State.STARTED) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignore) {}
    }
  }

  @Test
  public void testMetricInitialization() {
    PlatformService service = getService("test-service",
        Map.of("service.admin.port", "59001"));
    startService(service);

    Assertions.assertEquals("test-service", service.getServiceName());
    Assertions.assertTrue(service.healthCheck());

    // Verify that the metric registry is initialized and `/metrics` endpoint is working.
    HttpClient httpclient = HttpClients.createDefault();
    HttpGet metricsGet = new HttpGet("http://localhost:59001/metrics");
    try {
      HttpResponse response = httpclient.execute(metricsGet);
      Assertions.assertTrue(response.getEntity().getContentType().getValue().startsWith("text/plain;"));
      String responseStr = EntityUtils.toString(response.getEntity());
      EntityUtils.consume(response.getEntity());

      // Verify that some key JVM metrics are present in the response.
      Assertions.assertTrue(responseStr.contains("jvm_memory_used_bytes{app=\"test-service\",area=\"heap\""));
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.fail("Unexpected exception: " + e.getMessage());
    }

    service.shutdown();

    // Metrics endpoint should be down now.
    try {
      httpclient.execute(metricsGet);
      Assertions.fail("Expecting an exception here.");
    } catch (IOException ignore) {
      // Expected
    }
  }

  @Test
  public void testMetricInitializationWithDefaultTags() {
    PlatformService service = getService("test-service2",
        Map.of("service.admin.port", "59002", "metrics.defaultTags", "foo,bar,k1,v1,k2")
    );
    startService(service);

    Assertions.assertEquals("test-service2", service.getServiceName());
    Assertions.assertTrue(service.healthCheck());

    // Verify that the metric registry is initialized and `/metrics` endpoint is working.
    HttpClient httpclient = HttpClients.createDefault();
    HttpGet metricsGet = new HttpGet("http://localhost:59002/metrics");
    try {
      HttpResponse response = httpclient.execute(metricsGet);
      Assertions.assertTrue(response.getEntity().getContentType().getValue().startsWith("text/plain;"));
      String responseStr = EntityUtils.toString(response.getEntity());
      EntityUtils.consume(response.getEntity());
      System.out.println(responseStr);

      // Verify that some key JVM metrics are present in the response.
      Assertions.assertTrue(responseStr.contains("jvm_memory_used_bytes{app=\"test-service2\",area=\"heap\",foo=\"bar\""));
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.fail("Unexpected exception: " + e.getMessage());
    }

    service.shutdown();
  }
}
