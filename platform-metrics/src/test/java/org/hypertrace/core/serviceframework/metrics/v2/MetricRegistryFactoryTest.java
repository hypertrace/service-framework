package org.hypertrace.core.serviceframework.metrics.v2;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;
import org.apache.flink.api.common.TaskInfo;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.hypertrace.core.serviceframework.metrics.flink.SharedMeterRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.micrometer.MicrometerMetricRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MetricRegistryFactoryTest {

  @BeforeAll
  static void initSharedRegistry() throws IOException {
    SharedMeterRegistry.getOrCreate("test-service", freePort());
  }

  @Test
  void testForSubtaskReturnsMicrometerRegistry() {
    RuntimeContext context = mock(RuntimeContext.class);
    TaskInfo taskInfo = mock(TaskInfo.class);
    when(context.getTaskInfo()).thenReturn(taskInfo);
    when(taskInfo.getTaskName()).thenReturn("enricher");
    when(taskInfo.getIndexOfThisSubtask()).thenReturn(0);

    MetricRegistry registry = MetricRegistryFactory.forSubtask(context);

    assertNotNull(registry);
    assertInstanceOf(MicrometerMetricRegistry.class, registry);
  }

  @Test
  void testForJvmReturnsMicrometerRegistry() {
    MetricRegistry registry = MetricRegistryFactory.forJvm();

    assertNotNull(registry);
    assertInstanceOf(MicrometerMetricRegistry.class, registry);
  }

  @Test
  void testForJvmReturnsSharedInstanceAcrossCalls() {
    MetricRegistry first = MetricRegistryFactory.forJvm();
    MetricRegistry second = MetricRegistryFactory.forJvm();

    assertSame(first, second);
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
