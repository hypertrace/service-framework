package org.hypertrace.core.serviceframework.metrics.registry;

import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.hypertrace.core.serviceframework.metrics.config.PrometheusPushRegistryConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PrometheusPushMeterRegistryTest {
  private static final String JOB_NAME = "unit-test";
  private static final long pushIntervalInMillis = 100L;
  private PrometheusPushMeterRegistry pushMeterRegistry;

  @Test
  public void test_init_metric_pushed() throws InterruptedException, IOException {
    PushGateway mockPushGateway = Mockito.mock(PushGateway.class);
    pushMeterRegistry = new PrometheusPushMeterRegistry(new PrometheusPushRegistryConfig() {
      @Override
      public String jobName() {
        return JOB_NAME;
      }

      @Override
      public String prefix() {
        return "test";
      }

      @Override
      public String get(String key) {
        return null;
      }

      @Override
      public Duration step() {
        return Duration.ofMillis(pushIntervalInMillis);
      }

    }, Executors.defaultThreadFactory(), mockPushGateway);
    sleep(500);
    verify(mockPushGateway, atLeastOnce()).pushAdd(any(CollectorRegistry.class), eq(JOB_NAME));
    pushMeterRegistry.stop();
  }
}
