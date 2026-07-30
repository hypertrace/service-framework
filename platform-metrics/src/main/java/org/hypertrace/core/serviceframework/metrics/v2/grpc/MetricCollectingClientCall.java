package org.hypertrace.core.serviceframework.metrics.v2.grpc;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;

/**
 * Forwarding client call that counts request messages as they are sent and starts the
 * processing-duration clock when the call begins, handing it off to the response listener.
 *
 * @param <Q> the request message type
 * @param <A> the response message type
 */
class MetricCollectingClientCall<Q, A> extends SimpleForwardingClientCall<Q, A> {
  private final MetricSet metrics;
  private final long startNanos = System.nanoTime();

  MetricCollectingClientCall(ClientCall<Q, A> delegate, MetricSet metrics) {
    super(delegate);
    this.metrics = metrics;
  }

  @Override
  public void start(Listener<A> responseListener, Metadata metadata) {
    super.start(
        new MetricCollectingClientCallListener<>(responseListener, metrics, startNanos), metadata);
  }

  @Override
  public void sendMessage(Q message) {
    metrics.incrementRequestsSent();
    super.sendMessage(message);
  }
}
