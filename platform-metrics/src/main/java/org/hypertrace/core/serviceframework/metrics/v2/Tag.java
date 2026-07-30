package org.hypertrace.core.serviceframework.metrics.v2;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class Tag {
  private final String key;
  private final String value;
}
