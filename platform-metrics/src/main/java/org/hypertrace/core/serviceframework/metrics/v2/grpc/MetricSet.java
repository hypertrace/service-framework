package org.hypertrace.core.serviceframework.metrics.v2.grpc;

import io.grpc.Status;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hypertrace.core.serviceframework.metrics.v2.Counter;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;
import org.hypertrace.core.serviceframework.metrics.v2.Timer;

/**
 * Meters shared by every call of a single gRPC method: the request/response message counters and
 * the processing-duration timer. Encapsulates how each observation maps onto the underlying meters
 * (including the {@code statusCode} dimension) so the forwarding call and listener stay agnostic of
 * metric naming and tagging.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class MetricSet {
  private static final String TAG_STATUS_CODE = "statusCode";

  private final Counter requestCounter;
  private final Counter responseCounter;
  private final Timer processingTimer;

  void incrementRequestsSent() {
    requestCounter.increment();
  }

  void incrementResponsesReceived() {
    responseCounter.increment();
  }

  void recordProcessingDuration(Duration duration, Status.Code statusCode) {
    processingTimer.record(duration, new Tag(TAG_STATUS_CODE, statusCode.name()));
  }
}
