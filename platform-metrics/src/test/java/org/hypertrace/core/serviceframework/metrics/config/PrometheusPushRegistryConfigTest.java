package org.hypertrace.core.serviceframework.metrics.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.config.validate.ValidationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class PrometheusPushRegistryConfigTest {
  private static final String URL_ADDRESS = "localhost:9091";
  private static final int INTERVAL_REPORT_SEC = 1;
  private static final String JOB_NAME = "job";
  private PrometheusPushRegistryConfig config;

  @Test
  public void test_config_validData_validateSuccessfully() {
    config = create(URL_ADDRESS, INTERVAL_REPORT_SEC, JOB_NAME);
    config.requireValid();
  }

  @Test
  public void test_config_nullJobName_shouldFailValidation() {
    config = create(URL_ADDRESS, INTERVAL_REPORT_SEC, null);
    assertThrows(ValidationException.class, () -> config.requireValid());
  }

  public PrometheusPushRegistryConfig create(
      String urlAddress, int intervalReportSec, String jobName) {
    return new PrometheusPushRegistryConfig() {
      @Override
      public String jobName() {
        return jobName;
      }

      @Override
      public String prefix() {
        return "test";
      }

      @Override
      public String get(String key) {
        return null;
      }

      @Override
      public Duration step() {
        return Duration.ofSeconds(intervalReportSec);
      }
    };
  }
}
