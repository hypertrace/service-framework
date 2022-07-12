package org.hypertrace.core.serviceframework.grpc;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlatformPeriodicTaskDefinition {
  Runnable runnable;
  Duration initialDelay;
  Duration period;
  String name;
}
