package org.hypertrace.core.serviceframework.metrics.v2.grpc;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;
import java.time.Duration;

/**
 * Forwarding client call listener that counts response messages as they arrive and records the
 * end-to-end processing duration (tagged with the terminal status code) when the call closes.
 *
 * @param <A> the response message type
 */
class MetricCollectingClientCallListener<A> extends SimpleForwardingClientCallListener<A> {
  private final MetricSet metrics;
  private final long startNanos;

  MetricCollectingClientCallListener(
      ClientCall.Listener<A> delegate, MetricSet metrics, long startNanos) {
    super(delegate);
    this.metrics = metrics;
    this.startNanos = startNanos;
  }

  @Override
  public void onMessage(A message) {
    metrics.incrementResponsesReceived();
    super.onMessage(message);
  }

  @Override
  public void onClose(Status status, Metadata metadata) {
    metrics.recordProcessingDuration(
        Duration.ofNanos(System.nanoTime() - startNanos), status.getCode());
    super.onClose(status, metadata);
  }
}
