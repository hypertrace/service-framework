package org.hypertrace.core.serviceframework.docstore.metrics;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.micrometer.common.lang.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.metric.DocStoreMetric;
import org.hypertrace.core.documentstore.metric.DocStoreMetricProvider;
import org.hypertrace.core.documentstore.model.config.CustomMetricConfig;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;

@SuppressWarnings("unused")
public class DocStoreMetricsRegistry {
  private static final long INITIAL_DELAY_SECONDS = MINUTES.toSeconds(5);

  private final DocStoreMetricProvider metricProvider;
  @Nullable private PlatformServiceLifecycle platformLifecycle;
  private int threadPoolSize;
  private List<DocStoreCustomMetricReportingConfig> customMetricConfigs;
  private Duration standardMetricReportingInterval;

  public DocStoreMetricsRegistry(final Datastore datastore) {
    metricProvider = datastore.getDocStoreMetricProvider();
    platformLifecycle = null;
    threadPoolSize = 1;
    customMetricConfigs = emptyList();
    standardMetricReportingInterval = Duration.ofMinutes(30);
  }

  public DocStoreMetricsRegistry withPlatformLifecycle(
      final PlatformServiceLifecycle platformLifecycle) {
    this.platformLifecycle = platformLifecycle;
    return this;
  }

  public DocStoreMetricsRegistry withCustomMetrics(
      final List<DocStoreCustomMetricReportingConfig> customMetricConfigs) {
    this.customMetricConfigs = customMetricConfigs;
    return this;
  }

  public DocStoreMetricsRegistry withThreadPoolSize(final int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
    return this;
  }

  public DocStoreMetricsRegistry withStandardMetricReportingInterval(
      final Duration standardMetricReportingInterval) {
    this.standardMetricReportingInterval = standardMetricReportingInterval;
    return this;
  }

  /**
   * Continuously monitor a database and periodically report (standard and custom) metrics.
   *
   * <p>The standard metrics (like the database connection count this service is holding) are
   * reported immediately after this method is invoked and subsequently reported once in every 24
   * hours.
   *
   * <p>The custom metrics are reported at the interval scheduled per metric.
   */
  public void monitor() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadPoolSize);

    addShutdownHook(executor);

    new StandardDocStoreMetricsRegistry(executor).monitor();
    monitorCustomMetrics(executor);
  }

  /** Instantly query the datastore and report the custom metric once */
  public void report(final CustomMetricConfig customMetricConfig) {
    metricProvider.getCustomMetrics(customMetricConfig).forEach(this::report);
  }

  private void addShutdownHook(final ScheduledExecutorService executor) {
    if (platformLifecycle != null) {
      platformLifecycle.shutdownComplete().thenRun(executor::shutdown);
    }
  }

  private void monitorCustomMetrics(final ScheduledExecutorService executor) {
    customMetricConfigs.forEach(
        reportingConfig ->
            executor.scheduleAtFixedRate(
                () -> report(reportingConfig.config()),
                INITIAL_DELAY_SECONDS,
                reportingConfig.reportingInterval().toSeconds(),
                SECONDS));
  }

  private void report(final DocStoreMetric metric) {
    PlatformMetricsRegistry.registerGauge(metric.name(), metric.labels(), metric.value());
  }

  private class StandardDocStoreMetricsRegistry {
    private final ScheduledExecutorService executor;
    private final AtomicLong connectionCount;

    public StandardDocStoreMetricsRegistry(final ScheduledExecutorService executor) {
      this.executor = executor;
      this.connectionCount = registerConnectionCountMetric();
    }

    private void monitor() {
      executor.scheduleAtFixedRate(
          this::queryDocStoreAndSetMetricValues,
          INITIAL_DELAY_SECONDS,
          standardMetricReportingInterval.toSeconds(),
          SECONDS);
    }

    private AtomicLong registerConnectionCountMetric() {
      final DocStoreMetric docStoreMetric = metricProvider.getConnectionCountMetric();
      return PlatformMetricsRegistry.registerGauge(
          docStoreMetric.name(),
          docStoreMetric.labels(),
          new AtomicLong(castToLong(docStoreMetric.value())));
    }

    private void queryDocStoreAndSetMetricValues() {
      connectionCount.set(castToLong(metricProvider.getConnectionCountMetric().value()));
    }

    private long castToLong(final double value) {
      return Double.valueOf(value).longValue();
    }
  }
}
