package org.hypertrace.core.serviceframework.metrics.v2;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.TaskInfo;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;
import org.hypertrace.core.serviceframework.metrics.flink.SharedMeterRegistry;
import org.hypertrace.core.serviceframework.metrics.v2.micrometer.MicrometerMetricRegistry;

/** Entry point for obtaining {@link MetricRegistry} instances scoped to a given lifecycle. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricRegistryFactory {
  private static final String TASK_NAME = "task.name";
  private static final String SUBTASK_INDEX = "subtask.index";

  private static MetricRegistry jvmInstance;

  /**
   * Returns a registry scoped to a single Flink subtask, reporting through the Flink metrics
   * system.
   *
   * @param taskContext the Flink runtime context of the owning subtask
   * @return a registry bound to the subtask's metric group
   */
  public static MetricRegistry forSubtask(RuntimeContext taskContext) {
    return MicrometerMetricRegistry.create(
        PlatformMetricsRegistry.getMeterRegistry(), getTagsForSubtask(taskContext.getTaskInfo()));
  }

  /**
   * Returns a registry scoped to the whole JVM, reporting through Micrometer with no scope tags.
   *
   * <p>The view is created lazily and exactly once per JVM over the {@link SharedMeterRegistry}
   * backend; every caller in the process shares the single instance created by the first caller.
   *
   * @return a JVM-wide registry
   * @throws IllegalStateException if the shared meter registry has not been initialized yet
   */
  public static synchronized MetricRegistry forJvm() {
    if (jvmInstance == null) {
      jvmInstance = MicrometerMetricRegistry.create(PlatformMetricsRegistry.getMeterRegistry());
    }
    return jvmInstance;
  }

  private static Tag[] getTagsForSubtask(TaskInfo taskInfo) {
    return new Tag[] {
      new Tag(TASK_NAME, taskInfo.getTaskName()),
      new Tag(SUBTASK_INDEX, String.valueOf(taskInfo.getIndexOfThisSubtask()))
    };
  }
}
