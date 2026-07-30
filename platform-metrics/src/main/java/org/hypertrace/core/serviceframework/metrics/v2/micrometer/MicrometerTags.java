package org.hypertrace.core.serviceframework.metrics.v2.micrometer;

import io.micrometer.core.instrument.Tags;
import org.hypertrace.core.serviceframework.metrics.v2.Tag;

/** Converts our {@link Tag}s into Micrometer {@link Tags}. */
final class MicrometerTags {
  private MicrometerTags() {}

  static Tags of(Tag[] tags) {
    io.micrometer.core.instrument.Tag[] converted =
        new io.micrometer.core.instrument.Tag[tags.length];
    for (int i = 0; i < tags.length; i++) {
      converted[i] = io.micrometer.core.instrument.Tag.of(tags[i].getKey(), tags[i].getValue());
    }
    return Tags.of(converted);
  }
}
