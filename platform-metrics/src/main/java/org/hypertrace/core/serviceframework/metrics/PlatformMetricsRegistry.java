package org.hypertrace.core.serviceframework.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.cache.Cache;
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlatformMetricsRegistry is the MetricRegistry used for all the metrics. Framework will take care
 * of metrics exporters.
 */
public class PlatformMetricsRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMetricsRegistry.class);
  private static final int METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT = 30;
  public static final String DEFAULT_METRICS_PREFIX = "org.hypertrace.core.serviceframework";

  private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();
  private static ConsoleReporter consoleReporter;
  private static String metricsPrefix;
  public static final List<String> DEFAULT_METRICS_REPORTERS = List.of("prometheus");
  private static final Map<String, MetricSet> DEFAULT_METRIC_SET = new HashMap<>() {{
    put("gc", new GarbageCollectorMetricSet());
    put("jvm", new JvmAttributeGaugeSet());
    put("memory", new MemoryUsageGaugeSet());
    put("thread", new ThreadStatesGaugeSet());
  }};
  private static boolean isInit = false;
  private static final Set<Tag> DEFAULT_TAGS = new HashSet<>();

  /**
   * Main MetricMeter registry, with which all the metrics should be registered. We use
   * a {@link CompositeMeterRegistry} here so that we can even registry multiple registries
   * like Prometheus and Logging registries, if needed.
   */
  private static final CompositeMeterRegistry METER_REGISTRY = new CompositeMeterRegistry();

  private static void initPrometheusExporter(int reportInterval) {
    LOGGER.info("Trying to init PrometheusReporter");

    // Add Prometheus registry to the composite registry.
    METER_REGISTRY.add(new PrometheusMeterRegistry(new PrometheusConfig() {
      @Override
      @Nonnull
      public Duration step() {
        return Duration.ofSeconds(reportInterval);
      }

      @Override
      @io.micrometer.core.lang.Nullable
      public String get(String k) {
        return null;
      }
    }, CollectorRegistry.defaultRegistry, Clock.SYSTEM));

    CollectorRegistry.defaultRegistry.register(new DropwizardExports(METRIC_REGISTRY));
  }

  private static void initConsoleMetricsReporter(final int reportIntervalSec) {
    consoleReporter = ConsoleReporter.forRegistry(METRIC_REGISTRY).build();
    LOGGER
        .info("Trying to init ConsoleReporter with reporter interval=[{}] seconds", reportIntervalSec);
    consoleReporter.start(reportIntervalSec, TimeUnit.SECONDS);
  }

  private static void initLoggingMetricsReporter(int reportIntervalSec) {
    LOGGER.info("Initializing the logging metric reporter.");

    METER_REGISTRY.add(new LoggingMeterRegistry(new LoggingRegistryConfig() {
      @Override
      @Nonnull
      public Duration step() {
        return Duration.ofSeconds(reportIntervalSec);
      }

      @Override
      @io.micrometer.core.lang.Nullable
      public String get(String key) {
        return null;
      }
    }, Clock.SYSTEM));
  }

  public synchronized static void initMetricsRegistry(String serviceName, Map<String, String> tags) {
    initMetricsRegistry(serviceName, DEFAULT_METRICS_REPORTERS, DEFAULT_METRICS_PREFIX,
        METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT, tags);
  }

  public synchronized static void initMetricsRegistry(String serviceName,
      final List<String> reporters, final String prefix,
      final int reportIntervalSec, Map<String, String> tags) {
    if (isInit) {
      return;
    }

    metricsPrefix = prefix;

    for (String reporter : reporters) {
      switch (reporter.toLowerCase()) {
        case "console":
          initConsoleMetricsReporter(reportIntervalSec);
          break;
        case "logging":
          initLoggingMetricsReporter(reportIntervalSec);
          break;
        case "prometheus":
          initPrometheusExporter(reportIntervalSec);
          break;
        default:
          LOGGER.warn("Cannot find metric reporter: {}", reporter);
      }
    }

    // Add the service name and other given tags to the default tags list.
    DEFAULT_TAGS.add(new ImmutableTag("app", serviceName));
    tags.forEach((key, value) -> DEFAULT_TAGS.add(new ImmutableTag(key, value)));

    // Register different metrics with the registry.
    new ClassLoaderMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmGcMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new ProcessorMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmThreadMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);
    new JvmMemoryMetrics(DEFAULT_TAGS).bindTo(METER_REGISTRY);

    new ProcessMemoryMetrics().bindTo(METER_REGISTRY);
    new ProcessThreadMetrics().bindTo(METER_REGISTRY);

    for (String key : DEFAULT_METRIC_SET.keySet()) {
      METRIC_REGISTRY
          .registerAll(String.format("%s.%s", metricsPrefix, key), DEFAULT_METRIC_SET.get(key));
    }
    isInit = true;
  }

  /**
   * This method is deprecated since we'll be removing the Dropwizard metrics support in
   * future releases.
   */
  @Deprecated
  public static void register(String metricName, Metric metric) {
    final String fullMetricName = String.format("%s.%s", metricsPrefix, metricName);
    if (!METRIC_REGISTRY.getNames().contains(fullMetricName)) {
      METRIC_REGISTRY.register(fullMetricName, metric);
    }
  }

  /**
   * Registers a Counter with the given name with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/concepts#_counters for more details on the Counter.
   */
  public static Counter registerCounter(String name, Map<String, String> tags) {
    return METER_REGISTRY.counter(name, addDefaultTags(tags));
  }

  /**
   * Registers a Timer (without histograms) with the given name with the service's metric registry
   * and reports it periodically to the configured reporters. Apart from the given tags, the
   * reporting service's default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/concepts#_timers for more details on the Timer.
   */
  public static Timer registerTimer(String name, Map<String, String> tags) {
    return registerTimer(name, tags, false);
  }

  /**
   * Registers a Timer with the given name with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/concepts#_timers for more details on the Timer.
   */
  public static Timer registerTimer(String name, Map<String, String> tags, boolean histogram) {
    Timer.Builder builder = Timer.builder(name)
        .publishPercentiles(0.5, 0.95, 0.99)
        .tags(addDefaultTags(tags));
    if (histogram) {
      builder = builder.publishPercentileHistogram();
    }
    return builder.register(METER_REGISTRY);
  }

  /**
   * Registers the given number as a Guage with the service's metric registry and reports it
   * periodically to the configured reporters. Apart from the given tags, the reporting service's
   * default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/concepts#_gauges for more details on the Gauges.
   */
  public static <T extends Number> T registerGauge(String name, Map<String, String> tags, T number) {
    return METER_REGISTRY.gauge(name, addDefaultTags(tags), number);
  }

  /**
   * Registers metrics for the given Guava Cache with the service's metric registry and
   * reports them periodically to the configured reporters. Apart from the given tags, the
   * reporting service's default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/ref/cache for more details on the metrics.
   */
  public static void monitorGuavaCache(String name, Cache cache,
      @javax.annotation.Nullable Map<String, String> tags) {
    GuavaCacheMetrics.monitor(METER_REGISTRY, cache, name, addDefaultTags(tags));
  }

  /**
   * Registers metrics for the given executor service with the service's metric registry and
   * reports them periodically to the configured reporters. Apart from the given tags, the
   * reporting service's default tags also will be reported with the metrics.
   *
   * See https://micrometer.io/docs/ref/jvm for more details on the metrics.
   */
  public static void monitorExecutorService(String name, ExecutorService executorService,
      @javax.annotation.Nullable Map<String, String> tags) {
    new ExecutorServiceMetrics(executorService, name, addDefaultTags(tags)).bindTo(METER_REGISTRY);
  }

  private static Iterable<Tag> addDefaultTags(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return DEFAULT_TAGS;
    }

    Set<Tag> newTags = new HashSet<>(DEFAULT_TAGS);
    tags.forEach((k, v) -> newTags.add(new ImmutableTag(k, v)));
    return newTags;
  }

  public static MetricRegistry getMetricRegistry() {
    return METRIC_REGISTRY;
  }

  public static MeterRegistry getMeterRegistry() {
    return METER_REGISTRY;
  }

  public static void stop() {
    stopConsoleMetricsReporter();
  }

  private static void stopConsoleMetricsReporter() {
    if (consoleReporter == null) {
      return;
    }
    // report all metrics one final time
    consoleReporter.report();
    // stop console reporter
    consoleReporter.stop();
  }
}
