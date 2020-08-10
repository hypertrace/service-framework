package org.hypertrace.core.serviceframework.metrics;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Collector.Type;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Metrics servlet that will be used to expose metrics. The logic is adapted from {@link
 * io.prometheus.client.exporter.MetricsServlet}
 * <p>
 * flink-metrics-prometheus module contains a shaded version of prometheus client and register
 * metrics(collectors) to the shaded version of {@link CollectorRegistry} which is {@link
 * org.apache.flink.shaded.io.prometheus.client.CollectorRegistry}. The metrics servlet essentially
 * goes through the collector registry and iterates through all the collectors and invokes {@link
 * io.prometheus.client.Collector.collect()} which returns the metric samples.
 * <p>
 * For flink metrics we explicitly use the shaded CollectorRegistry instance and report the metrics
 * by adapting them from the shaded package to the expected package
 */
public class MetricsServlet extends HttpServlet {

  private static final String NAME_PARAM_KEY = "name[]";

  private final CollectorRegistry registry;
  private final org.apache.flink.shaded.io.prometheus.client.CollectorRegistry flinkRegistry;

  public MetricsServlet() {
    this.registry = CollectorRegistry.defaultRegistry;
    this.flinkRegistry = org.apache.flink.shaded.io.prometheus.client.CollectorRegistry.defaultRegistry;
  }

  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType(TextFormat.CONTENT_TYPE_004);

    try (Writer writer = resp.getWriter()) {
      Set<String> includedNames = parse(req);
      // first write all the dropwizard metric samples
      TextFormat.write004(writer, registry.filteredMetricFamilySamples(includedNames));
      // next adapt and write the flink metric samples
      TextFormat.write004(writer,
          adaptShadedFlinkMetricSamples(flinkRegistry.filteredMetricFamilySamples(includedNames)));
      writer.flush();
    }
  }

  /**
   * Adapts flink shaded metric samples
   */
  private Enumeration<MetricFamilySamples> adaptShadedFlinkMetricSamples(
      Enumeration<org.apache.flink.shaded.io.prometheus.client.Collector.MetricFamilySamples> mfs) {
    List<MetricFamilySamples> adapted = new ArrayList<>();
    while (mfs.hasMoreElements()) {
      org.apache.flink.shaded.io.prometheus.client.Collector.MetricFamilySamples sample = mfs
          .nextElement();
      adapted.add(new MetricFamilySamples(sample.name, adaptType(sample.type), sample.help,
          adaptSamples(sample.samples)));
    }
    return Collections.enumeration(adapted);
  }

  private Type adaptType(org.apache.flink.shaded.io.prometheus.client.Collector.Type type) {
    switch (type) {
      case GAUGE:
        return Type.GAUGE;
      case COUNTER:
        return Type.COUNTER;
      case SUMMARY:
        return Type.SUMMARY;
      case HISTOGRAM:
        return Type.HISTOGRAM;
      case UNTYPED:
        return Type.UNTYPED;
      default:
        throw new IllegalArgumentException(String.format(
            "type=%s mismatch. Ensure the prometheus client and shaded versions are compatible",
            type));
    }
  }

  private List<Sample> adaptSamples(
      List<org.apache.flink.shaded.io.prometheus.client.Collector.MetricFamilySamples.Sample> samples) {
    List<Sample> adaptedSamples = new ArrayList<>();
    for (org.apache.flink.shaded.io.prometheus.client.Collector.MetricFamilySamples.Sample sample : samples) {
      adaptedSamples.add(
          new Sample(sample.name, sample.labelNames, sample.labelValues, sample.value,
              sample.timestampMs));
    }
    return adaptedSamples;
  }

  private Set<String> parse(HttpServletRequest req) {
    String[] includedParam = req.getParameterValues(NAME_PARAM_KEY);
    if (includedParam == null) {
      return Collections.emptySet();
    } else {
      return new HashSet<String>(Arrays.asList(includedParam));
    }
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {
    doGet(req, resp);
  }

}
