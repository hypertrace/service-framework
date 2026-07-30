package org.hypertrace.core.serviceframework.metrics.v2.grpc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hypertrace.core.serviceframework.metrics.v2.Counter;
import org.hypertrace.core.serviceframework.metrics.v2.MetricRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.StrictRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;
import org.hypertrace.core.serviceframework.metrics.v2.Timer;
import org.junit.jupiter.api.Test;

class MetricCollectingClientInterceptorTest {

  private static final MethodDescriptor<String, String> METHOD =
      MethodDescriptor.<String, String>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(MethodDescriptor.generateFullMethodName("test.EchoService", "Echo"))
          .setRequestMarshaller(StringMarshaller.INSTANCE)
          .setResponseMarshaller(StringMarshaller.INSTANCE)
          .build();

  @Test
  void testRequestCounterIncrementsOnSendMessage() {
    RecordingRegistry registry = new RecordingRegistry();
    CapturingClientCall<String, String> delegate = new CapturingClientCall<>();
    ClientCall<String, String> call = intercept(registry, delegate);

    call.sendMessage("a");
    call.sendMessage("b");

    assertEquals(2L, registry.counter("grpc.client.requests.sent").total);
    assertEquals(2, delegate.sentMessages.size());
  }

  @Test
  void testResponseCounterIncrementsOnMessage() {
    RecordingRegistry registry = new RecordingRegistry();
    CapturingClientCall<String, String> delegate = new CapturingClientCall<>();
    ClientCall<String, String> call = intercept(registry, delegate);

    call.start(new NoopListener<>(), new Metadata());
    delegate.listener.onMessage("r1");
    delegate.listener.onMessage("r2");

    assertEquals(2L, registry.counter("grpc.client.responses.received").total);
  }

  @Test
  void testProcessingDurationRecordedOnCloseWithStatusCode() {
    RecordingRegistry registry = new RecordingRegistry();
    CapturingClientCall<String, String> delegate = new CapturingClientCall<>();
    ClientCall<String, String> call = intercept(registry, delegate);

    call.start(new NoopListener<>(), new Metadata());
    delegate.listener.onClose(Status.OK, new Metadata());

    RecordingTimer timer = registry.timer("grpc.client.processing.duration");
    assertEquals(1, timer.recordings.size());
    assertTrue(timer.recordings.get(0).tags.contains(new Tag("statusCode", "OK")));
  }

  @Test
  void testProcessingDurationTaggedWithErrorStatusCode() {
    RecordingRegistry registry = new RecordingRegistry();
    CapturingClientCall<String, String> delegate = new CapturingClientCall<>();
    ClientCall<String, String> call = intercept(registry, delegate);

    call.start(new NoopListener<>(), new Metadata());
    delegate.listener.onClose(Status.UNAVAILABLE, new Metadata());

    RecordingTimer timer = registry.timer("grpc.client.processing.duration");
    assertTrue(timer.recordings.get(0).tags.contains(new Tag("statusCode", "UNAVAILABLE")));
  }

  @Test
  void testMetersTaggedWithServiceMethodAndType() {
    RecordingRegistry registry = new RecordingRegistry();
    ClientCall<String, String> call = intercept(registry, new CapturingClientCall<>());
    call.sendMessage("a");
    call.start(new NoopListener<>(), new Metadata());

    List<Tag> expected =
        List.of(
            new Tag("service", "test.EchoService"),
            new Tag("method", "Echo"),
            new Tag("methodType", "UNARY"));
    assertEquals(expected, registry.counter("grpc.client.requests.sent").tags);
    assertEquals(expected, registry.counter("grpc.client.responses.received").tags);
    assertEquals(expected, registry.timer("grpc.client.processing.duration").tags);
  }

  @Test
  void testRepeatedInterceptionsDoNotCreateNewMetrics() {
    // Models the Flink backend, where registering the same (name, tags) meter twice throws.
    StrictRegistry registry = new StrictRegistry();
    MetricCollectingClientInterceptor interceptor = new MetricCollectingClientInterceptor(registry);

    // First interception registers the meters for this method.
    interceptor.interceptCall(
        METHOD, CallOptions.DEFAULT, new CapturingChannel(new CapturingClientCall<>()));

    // Intercepting the SAME method again must reuse the cached MetricSet, not re-register.
    assertDoesNotThrow(
        () ->
            interceptor.interceptCall(
                METHOD, CallOptions.DEFAULT, new CapturingChannel(new CapturingClientCall<>())));
  }

  private static ClientCall<String, String> intercept(
      RecordingRegistry registry, CapturingClientCall<String, String> delegate) {
    MetricCollectingClientInterceptor interceptor = new MetricCollectingClientInterceptor(registry);
    return interceptor.interceptCall(METHOD, CallOptions.DEFAULT, new CapturingChannel(delegate));
  }

  private static final class RecordingRegistry implements MetricRegistry {
    private final Map<String, RecordingCounter> counters = new HashMap<>();
    private final Map<String, RecordingTimer> timers = new HashMap<>();

    RecordingCounter counter(String name) {
      return counters.get(name);
    }

    RecordingTimer timer(String name) {
      return timers.get(name);
    }

    @Override
    public Counter counter(String name, Tag... tags) {
      return counters.computeIfAbsent(name, k -> new RecordingCounter(List.of(tags)));
    }

    @Override
    public Timer timer(String name, Tag... tags) {
      return timers.computeIfAbsent(name, k -> new RecordingTimer(List.of(tags)));
    }

    @Override
    public <T> void gauge(
        String name, T state, java.util.function.ToDoubleFunction<T> f, Tag... t) {
      // not exercised by this interceptor
    }

    @Override
    public <T> void asyncCounter(
        String name, T state, java.util.function.ToLongFunction<T> f, Tag... t) {
      // not exercised by this interceptor
    }
  }

  private static final class RecordingCounter implements Counter {
    private final List<Tag> tags;
    private long total;

    RecordingCounter(List<Tag> tags) {
      this.tags = tags;
    }

    @Override
    public void increment(long amount, Tag... runtimeTags) {
      total += amount;
    }
  }

  private static final class RecordingTimer implements Timer {
    private final List<Tag> tags;
    private final List<Recording> recordings = new ArrayList<>();

    RecordingTimer(List<Tag> tags) {
      this.tags = tags;
    }

    @Override
    public void record(Duration duration, Tag... runtimeTags) {
      recordings.add(new Recording(duration, List.of(runtimeTags)));
    }
  }

  private static final class Recording {
    private final Duration duration;
    private final List<Tag> tags;

    Recording(Duration duration, List<Tag> tags) {
      this.duration = duration;
      this.tags = tags;
    }
  }

  /** Captures the wrapped response listener and sent messages passed to the delegate call. */
  private static final class CapturingClientCall<Q, A> extends ClientCall<Q, A> {
    private final List<Q> sentMessages = new ArrayList<>();
    private Listener<A> listener;

    @Override
    public void start(Listener<A> responseListener, Metadata headers) {
      this.listener = responseListener;
    }

    @Override
    public void request(int numMessages) {
      // no-op
    }

    @Override
    public void cancel(String message, Throwable cause) {
      // no-op
    }

    @Override
    public void halfClose() {
      // no-op
    }

    @Override
    public void sendMessage(Q message) {
      sentMessages.add(message);
    }
  }

  private static final class CapturingChannel extends Channel {
    private final CapturingClientCall<String, String> call;

    CapturingChannel(CapturingClientCall<String, String> call) {
      this.call = call;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Q, A> ClientCall<Q, A> newCall(
        MethodDescriptor<Q, A> methodDescriptor, CallOptions callOptions) {
      return (ClientCall<Q, A>) call;
    }

    @Override
    public String authority() {
      return "test";
    }
  }

  private static final class NoopListener<A> extends ClientCall.Listener<A> {}

  private enum StringMarshaller implements MethodDescriptor.Marshaller<String> {
    INSTANCE;

    @Override
    public InputStream stream(String value) {
      return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String parse(InputStream stream) {
      try {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
