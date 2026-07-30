package org.hypertrace.core.serviceframework.metrics.v2.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hypertrace.core.serviceframework.metrics.v2.MetricRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;

/**
 * gRPC client interceptor that records per-method call metrics through a {@link MetricRegistry}.
 *
 * <p>For every intercepted method it reports the number of request messages sent, the number of
 * response messages received, and the end-to-end processing duration of the call. Each metric is
 * tagged with the called {@code service}, {@code method}, and {@code methodType}; the processing
 * duration is additionally tagged with the terminal {@code statusCode}.
 *
 * <p>This mirrors Micrometer's {@code MetricCollectingClientInterceptor} but emits through the
 * project's {@link MetricRegistry} abstraction so the same wiring works across the Micrometer,
 * Flink, and no-op backends. The per-call bookkeeping lives in {@link MetricCollectingClientCall}
 * and {@link MetricCollectingClientCallListener}, which delegate to the per-method {@link
 * MetricSet}.
 *
 * @param registry the registry metrics are reported to
 */
public class MetricCollectingClientInterceptor implements ClientInterceptor {

  private static final String METRIC_REQUESTS_SENT = "grpc.client.requests.sent";
  private static final String METRIC_RESPONSES_RECEIVED = "grpc.client.responses.received";
  private static final String METRIC_PROCESSING_DURATION = "grpc.client.processing.duration";

  private static final String TAG_SERVICE = "service";
  private static final String TAG_METHOD = "method";
  private static final String TAG_METHOD_TYPE = "methodType";

  private final MetricRegistry registry;

  // Caches one MetricSet per full method name. This is load-bearing, not just an optimization:
  // computeIfAbsent guarantees newMetricsFor (and thus registry.counter/timer) runs at most once
  // per method, so backends that reject duplicate (name, tags) registrations — e.g. Flink's
  // MetricGroup#counter — never see a second registration for the same method.
  private final Map<String, MetricSet> metricsForMethods = new ConcurrentHashMap<>();

  public MetricCollectingClientInterceptor(MetricRegistry registry) {
    this.registry = registry;
  }

  @Override
  public <Q, A> ClientCall<Q, A> interceptCall(
      MethodDescriptor<Q, A> method, CallOptions callOptions, Channel channel) {
    MetricSet metrics =
        metricsForMethods.computeIfAbsent(method.getFullMethodName(), k -> newMetricsFor(method));
    return new MetricCollectingClientCall<>(channel.newCall(method, callOptions), metrics);
  }

  private MetricSet newMetricsFor(MethodDescriptor<?, ?> method) {
    Tag service = new Tag(TAG_SERVICE, method.getServiceName());
    Tag methodName = new Tag(TAG_METHOD, method.getBareMethodName());
    Tag methodType = new Tag(TAG_METHOD_TYPE, method.getType().name());
    return new MetricSet(
        registry.counter(METRIC_REQUESTS_SENT, service, methodName, methodType),
        registry.counter(METRIC_RESPONSES_RECEIVED, service, methodName, methodType),
        registry.timer(METRIC_PROCESSING_DURATION, service, methodName, methodType));
  }
}
