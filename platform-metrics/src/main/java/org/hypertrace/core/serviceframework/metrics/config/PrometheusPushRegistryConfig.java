package org.hypertrace.core.serviceframework.metrics.config;

import static io.micrometer.core.instrument.config.MeterRegistryConfigValidator.checkAll;
import static io.micrometer.core.instrument.config.MeterRegistryConfigValidator.checkRequired;

import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.push.PushRegistryConfig;

public interface PrometheusPushRegistryConfig extends PushRegistryConfig {
  /**
   * Batch job name as groupingKey for metrics in PushGateway
   */
  String jobName();

  @Override
  default Validated<?> validate() {
    return checkAll(this,
        checkRequired("jobName", PrometheusPushRegistryConfig::jobName)
    );
  }
}
