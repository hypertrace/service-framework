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
import lombok.NonNull;
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
  private Duration standardMetricsReportingInterval;
  private ScheduledExecutorService executor;

  public DocStoreMetricsRegistry(@NonNull final Datastore datastore) {
    metricProvider = datastore.getDocStoreMetricProvider();
    platformLifecycle = null;
    threadPoolSize = 1;
    customMetricConfigs = emptyList();
    standardMetricsReportingInterval = Duration.ofMinutes(30);
  }

  /**
   * Supply the platform lifecycle to stop monitoring the datastore when the service is shutting
   * down
   */
  public DocStoreMetricsRegistry withPlatformLifecycle(
      final PlatformServiceLifecycle platformLifecycle) {
    this.platformLifecycle = platformLifecycle;
    return this;
  }

  /** Define the custom metrics to be reported */
  public DocStoreMetricsRegistry withCustomMetrics(
      @NonNull final List<DocStoreCustomMetricReportingConfig> customMetricConfigs) {
    this.customMetricConfigs = customMetricConfigs;
    return this;
  }

  /**
   * Override the number of threads dedicated for datastore metric reporting. The default value is
   * 1.
   */
  public DocStoreMetricsRegistry withThreadPoolSize(final int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
    return this;
  }

  /**
   * Override the reporting interval of the standard datastore metrics. The default value is 30
   * minutes.
   */
  public DocStoreMetricsRegistry withStandardMetricsReportingInterval(
      @NonNull final Duration standardMetricsReportingInterval) {
    this.standardMetricsReportingInterval = standardMetricsReportingInterval;
    return this;
  }

  /**
   * Continuously monitor a database and periodically report (standard and custom) metrics.
   *
   * <p>The standard metrics (like the database connection count this service is holding) are
   * reported immediately after this method is invoked and subsequently reported at the standard
   * metrics reporting interval (configurable using {@link #withStandardMetricsReportingInterval})
   *
   * <p>The custom metrics, provided through {@link #withCustomMetrics}, are reported immediately
   * after this method is invoked and subsequently reported at the interval scheduled per metric.
   */
  public void monitor() {
    shutdown();
    executor = Executors.newScheduledThreadPool(threadPoolSize);

    addShutdownHook();

    new StandardDocStoreMetricsRegistry().monitor();
    monitorCustomMetrics();
  }

  /** Instantly query the datastore and report the custom metric once */
  public void report(final CustomMetricConfig customMetricConfig) {
    metricProvider.getCustomMetrics(customMetricConfig).forEach(this::report);
  }

  /** Stop monitoring the database */
  public void shutdown() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  private void addShutdownHook() {
    if (platformLifecycle != null) {
      platformLifecycle.shutdownComplete().thenRun(this::shutdown);
    }
  }

  private void monitorCustomMetrics() {
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
    private final AtomicLong connectionCount;

    public StandardDocStoreMetricsRegistry() {
      this.connectionCount = registerConnectionCountMetric();
    }

    private void monitor() {
      executor.scheduleAtFixedRate(
          this::queryDocStoreAndSetMetricValues,
          INITIAL_DELAY_SECONDS,
          standardMetricsReportingInterval.toSeconds(),
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
