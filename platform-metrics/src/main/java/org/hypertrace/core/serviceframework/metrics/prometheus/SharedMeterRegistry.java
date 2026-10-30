package org.hypertrace.core.serviceframework.metrics.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Predicate;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

/**
 * Process-wide Prometheus scrape endpoint used by Flink TaskManagers.
 *
 * <p>Application meters still report through {@link PlatformMetricsRegistry#getMeterRegistry()},
 * which lands in {@link CollectorRegistry#defaultRegistry}. This class serves that registry plus
 * any registry handed to {@link #register(CollectorRegistry)} (such as the one owned by Flink's
 * metric reporter) from a single HTTP server. The server is bound exactly once per JVM: the port
 * from the first caller wins, and later callers reuse that instance.
 *
 * <p>Parameters are JDK / simpleclient types on purpose so this class can sit on Flink's
 * parent-first shared classpath without coupling that surface to typesafe-config.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SharedMeterRegistry {

  // No name-collision checks: exposed registries are owned by different components and are
  // registered before they hold any metrics.
  private static final CollectorRegistry EXPOSED = new CollectorRegistry(false);

  static {
    EXPOSED.register(new RegistryDelegate(CollectorRegistry.defaultRegistry));
  }

  private static HTTPServer server;

  /**
   * Initializes {@link PlatformMetricsRegistry} for Flink and binds the scrape endpoint on first
   * use.
   *
   * @param serviceName service name added to the common metric tags
   * @param port the port on which to publish the Prometheus scrape endpoint; 0 picks a free port
   */
  public static synchronized void start(String serviceName, int port) {
    PlatformMetricsRegistry.initMetricsRegistryForFlink(serviceName);
    if (server != null) {
      return;
    }
    try {
      server =
          new HTTPServer.Builder()
              .withPort(port)
              .withRegistry(EXPOSED)
              .withDaemonThreads(true)
              .build();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to start Prometheus exporter HTTP server", e);
    }
  }

  /**
   * Serves the metrics of {@code source} on the shared scrape endpoint.
   *
   * @return a handle to pass to {@link #unregister(Collector)}
   */
  public static synchronized Collector register(CollectorRegistry source) {
    return new RegistryDelegate(source).register(EXPOSED);
  }

  /** Stops serving the registry previously passed to {@link #register(CollectorRegistry)}. */
  public static synchronized void unregister(Collector handle) {
    EXPOSED.unregister(handle);
  }

  /**
   * Returns the port the scrape endpoint is bound to.
   *
   * @throws IllegalStateException if {@link #start(String, int)} has not been called
   */
  public static synchronized int boundPort() {
    if (server == null) {
      throw new IllegalStateException("Prometheus exporter HTTP server is not started");
    }
    return server.getPort();
  }

  private static final class RegistryDelegate extends Collector {
    private final CollectorRegistry source;

    private RegistryDelegate(CollectorRegistry source) {
      this.source = source;
    }

    @Override
    public List<MetricFamilySamples> collect() {
      return Collections.list(source.metricFamilySamples());
    }

    @Override
    public List<MetricFamilySamples> collect(Predicate<String> sampleNameFilter) {
      return Collections.list(source.filteredMetricFamilySamples(sampleNameFilter));
    }
  }
}
