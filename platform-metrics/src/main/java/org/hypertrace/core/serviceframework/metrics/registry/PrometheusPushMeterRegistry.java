package org.hypertrace.core.serviceframework.metrics.registry;

import io.micrometer.core.instrument.util.TimeUtils;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.PushGateway;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.hypertrace.core.serviceframework.metrics.config.PrometheusPushRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Metric Registry for Prometheus Push Gateway */
public class PrometheusPushMeterRegistry extends PrometheusMeterRegistry {
  private static final Logger logger = LoggerFactory.getLogger(PlatformMetricsRegistry.class);
  private final PrometheusPushRegistryConfig pushConfig;
  private final PushGateway pushGateway;

  @Nullable private ScheduledExecutorService scheduledExecutorService;

  public PrometheusPushMeterRegistry(
      PrometheusPushRegistryConfig pushConfig,
      ThreadFactory threadFactory,
      PushGateway pushGateway) {
    super(pushConfig::get);

    pushConfig.requireValid();

    this.pushConfig = pushConfig;
    this.pushGateway = pushGateway;

    start(threadFactory);
  }

  public void publish() throws IOException {
    pushGateway.pushAdd(getPrometheusRegistry(), pushConfig.jobName());
  }

  /** Catch uncaught exceptions thrown from {@link #publish()}. */
  private void publishSafely() {
    try {
      publish();
    } catch (Throwable e) {
      logger.warn(
          "Unexpected exception thrown while publishing metrics for "
              + this.getClass().getSimpleName(),
          e);
    }
  }

  public void start(ThreadFactory threadFactory) {
    if (scheduledExecutorService != null) return;

    if (pushConfig.enabled()) {
      logger.info(
          "publishing metrics for "
              + this.getClass().getSimpleName()
              + " every "
              + TimeUtils.format(pushConfig.step()));

      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
      scheduledExecutorService.scheduleAtFixedRate(
          this::publishSafely,
          pushConfig.step().toMillis(),
          pushConfig.step().toMillis(),
          TimeUnit.MILLISECONDS);
    }
  }

  public void stop() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
    }
  }

  @Override
  public void close() {
    if (pushConfig.enabled()) {
      publishSafely();
    }
    stop();
    super.close();
  }
}
