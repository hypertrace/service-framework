package org.hypertrace.core.serviceframework.metrics.flink;

import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

/**
 * Owns the process-wide Micrometer {@link MeterRegistry} and its Prometheus scrape-endpoint HTTP
 * server.
 *
 * <p>This is the shared metrics backend that every scoped {@code MetricRegistry} view reports
 * through, regardless of whether that view is JVM-wide or bound to a single Flink subtask. The
 * registry and exporter are created lazily and exactly once per JVM: binding the scrape port is a
 * process-wide side effect, so every caller in the process shares the single instance created by
 * the first caller. Keeping this out of any Guice module means wiring a registry into an injector
 * carries no static state and performs no I/O during injection.
 *
 * <p>The underlying Prometheus registry is also exposed via {@link #getPrometheusRegistry} so that
 * the Flink metric reporter can bridge Flink-native metrics into the same endpoint.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SharedMeterRegistry {

  private static boolean initialized = false;

  /**
   * Returns the process-wide meter registry, lazily starting the Prometheus registry and its
   * scrape-endpoint HTTP server on first use. The exporter port comes from the first caller; later
   * callers receive the already-started instance and their port is ignored.
   *
   * <p>The parameter is a primitive on purpose: this class is shared across the Flink system and
   * user-code classloaders (so the reporter and operators see one singleton), and passing a
   * primitive avoids coupling that shared surface to any config type.
   *
   * @param serviceName service name added to the common metric tags
   * @param exporterPort the port on which to publish the Prometheus scrape endpoint
   * @return the shared process-wide meter registry
   */
  public static synchronized MeterRegistry getOrCreate(String serviceName, int exporterPort) {
    if (!initialized) {
      PlatformMetricsRegistry.initMetricsRegistry(serviceName);
      try {
        HTTPServer.builder()
            .port(exporterPort)
            .registry(PlatformMetricsRegistry.getPrometheusRegistry())
            .buildAndStart();
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to start Prometheus exporter HTTP server", e);
      }
      initialized = true;
    }
    return PlatformMetricsRegistry.getMeterRegistry();
  }

  /**
   * Returns the already-created process-wide meter registry.
   *
   * <p>Unlike {@link #getOrCreate(String, int)} this never creates the registry or binds the scrape
   * port; it is a pure consumer accessor for callers that do not own the exporter port (e.g. Flink
   * operators). Initialization is the responsibility of {@link #getOrCreate(String, int)}, which
   * the Prometheus metric reporter invokes at TaskManager startup before any operator runs.
   *
   * @return the shared process-wide meter registry
   * @throws IllegalStateException if the registry has not been initialized via {@link
   *     #getOrCreate(String, int)} yet
   */
  public static synchronized MeterRegistry get() {
    if (!initialized) {
      throw new IllegalStateException(
          "SharedMeterRegistry has not been initialized; getOrCreate(int) must run "
              + "(via the Prometheus metric reporter) before the registry can be consumed");
    }
    return PlatformMetricsRegistry.getMeterRegistry();
  }

  /**
   * Returns the underlying Prometheus registry backing the shared meter registry, starting the
   * shared registry and its exporter if they have not been created yet.
   *
   * @param serviceName service name added to the common metric tags
   * @param exporterPort the port on which to publish the Prometheus scrape endpoint
   * @return the shared Prometheus registry
   */
  public static synchronized PrometheusRegistry getPrometheusRegistry(
      String serviceName, int exporterPort) {
    getOrCreate(serviceName, exporterPort);
    return PlatformMetricsRegistry.getPrometheusRegistry();
  }
}
