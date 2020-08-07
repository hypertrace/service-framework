package org.hypertrace.core.serviceframework;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlatformService}
 */
public class PlatformServiceTest {
  private final CountDownLatch serviceStartSignal = new CountDownLatch(1);

  /**
   * A test service implementation.
   */
  static class TestService extends PlatformService {
    private final CountDownLatch latch;

    public TestService(ConfigClient client, CountDownLatch latch) {
      super(client);
      this.latch = latch;
    }

    @Override
    protected void doInit() {

    }

    @Override
    protected void doStart() {
      latch.countDown();
    }

    @Override
    protected void doStop() {

    }

    @Override
    public boolean healthCheck() {
      return true;
    }

    @Override
    public String getServiceName() {
      return "test-service";
    }
  }

  @Test
  public void testMetricInitialization() {
    PlatformService service = new TestService(new ConfigClient() {
      @Override
      public Config getConfig() {
        return ConfigFactory.parseMap(Map.of("service.admin.port", "59001"));
      }

      @Override
      public Config getConfig(String service, String cluster, String pod, String container) {
        return null;
      }
    }, serviceStartSignal);

    // Start the service in a separate thread since the thread which starts it waits until
    // the service is shutting down.
    Thread t = new Thread(() -> {
      service.initialize();
      service.start();
    });
    t.start();

    // Wait for the service to fully start so that admin server is started and all servlets
    // are initialized.
    try {
      serviceStartSignal.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Assertions.fail("Waited too long for the service to start.");
    }

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
    try {
      // Wait for the thread which started the service to go down.
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Metrics endpoint should be down now.
    try {
      httpclient.execute(metricsGet);
      Assertions.fail("Expecting an exception here.");
    } catch (IOException ignore) {
      // Expected
    }
  }
}
