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
import com.google.common.cache.CacheBuilder;
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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlatformMetricsRegistry is the MetricRegistry used for all the metrics. Framework will take care
 * of metrics exporters.
 */
public class PlatformMetricsRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMetricsRegistry.class);
  public static final int METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT = 30;
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
  private static final MeterRegistry METER_REGISTRY = new PrometheusMeterRegistry(new PrometheusConfig() {
    @Override
    public Duration step() {
      return Duration.ofSeconds(30);
    }

    @Override
    @Nullable
    public String get(String k) {
      return null;
    }
  }, CollectorRegistry.defaultRegistry, Clock.SYSTEM);

  private static void initPrometheusExporter() {
    LOGGER.info("Trying to init PrometheusReporter");
    CollectorRegistry.defaultRegistry.register(new DropwizardExports(METRIC_REGISTRY));
  }

  private static void initConsoleMetricsReporter() {
    initConsoleMetricsReporter(METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT);
  }

  private static void initConsoleMetricsReporter(final int reportInterval) {
    consoleReporter = ConsoleReporter.forRegistry(METRIC_REGISTRY).build();
    LOGGER
        .info("Trying to init ConsoleReporter with reporter interval=[{}] seconds", reportInterval);
    consoleReporter.start(reportInterval, TimeUnit.SECONDS);
  }

  public synchronized static void initMetricsRegistry(String serviceName, Map<String, String> tags) {
    initMetricsRegistry(serviceName, DEFAULT_METRICS_REPORTERS, DEFAULT_METRICS_PREFIX,
        METRICS_REPORTER_CONSOLE_REPORT_INTERVAL_DEFAULT, tags);
  }

  public synchronized static void initMetricsRegistry(String serviceName,
      final List<String> reporters, final String prefix,
      final int reportInterval, Map<String, String> tags) {
    if (isInit) {
      return;
    }

    metricsPrefix = prefix;

    for (String reporter : reporters) {

      switch (reporter.toLowerCase()) {
        case "console":
          initConsoleMetricsReporter(reportInterval);
          break;
        case "prometheus":
          initPrometheusExporter();
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

  public static void register(String metricName, Metric metric) {
    final String fullMetricName = String.format("%s.%s", metricsPrefix, metricName);
    if (!METRIC_REGISTRY.getNames().contains(fullMetricName)) {
      METRIC_REGISTRY.register(fullMetricName, metric);
    }
  }

  public static Counter registerCounter(String name, Map<String, String> tags) {
    return METER_REGISTRY.counter(String.format("%s.%s", metricsPrefix, name), addDefaultTags(tags));
  }

  public static Timer registerTimer(String name, Map<String, String> tags) {
    return registerTimer(name, tags, false);
  }

  public static Timer registerTimer(String name, Map<String, String> tags, boolean histogram) {
    Timer.Builder builder = Timer.builder(String.format("%s.%s", metricsPrefix, name))
        .publishPercentiles(0.5, 0.95, 0.99)
        .tags(addDefaultTags(tags));
    if (histogram) {
      builder = builder.publishPercentileHistogram();
    }
    return builder.register(METER_REGISTRY);
  }

  public static <T extends Number> T registerGauge(String name, Map<String, String> tags, T number) {
    return METER_REGISTRY.gauge(String.format("%s.%s", metricsPrefix, name), addDefaultTags(tags), number);
  }

  /**
   * Method to monitor the given Guava cache. You must call {@link CacheBuilder#recordStats()}
   * prior to building the cache for metrics to be recorded.
   */
  public static void monitorGuavaCache(String name, Cache cache, @javax.annotation.Nullable Map<String, String> tags) {
    GuavaCacheMetrics.monitor(METER_REGISTRY, cache, name, addDefaultTags(tags));
  }

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
