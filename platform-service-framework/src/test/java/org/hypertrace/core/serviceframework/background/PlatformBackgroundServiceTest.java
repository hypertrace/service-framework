package org.hypertrace.core.serviceframework.background;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.List;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class PlatformBackgroundServiceTest {
  @Test
  public void testPlatformBackgroundServiceMethods() throws Exception {
    ConfigClient configClient = mock(ConfigClient.class);
    Config config = mock(Config.class);
    when(config.getString("service.name")).thenReturn("test-service");
    when(configClient.getConfig()).thenReturn(config);
    Logger logger = mock(Logger.class);
    TestPlatformBackgroundService testService =
        new TestPlatformBackgroundService(configClient, logger);
    testService.doInit();

    Assertions.assertEquals("test-service", testService.getServiceName());
    Assertions.assertTrue(testService.healthCheck());

    testService.doStart();

    Assertions.assertEquals(2, testService.testList.size());
    Assertions.assertEquals(12, testService.testList.get(0).intValue());
    Assertions.assertEquals(23, testService.testList.get(1).intValue());

    testService.doStop();
    Assertions.assertTrue(testService.testList.isEmpty());

    Assertions.assertEquals(logger, testService.getLogger());
  }

  private class TestPlatformBackgroundJob implements PlatformBackgroundJob {
    private List<Integer> testList;
    private Config config;

    public TestPlatformBackgroundJob(List<Integer> testList, Config config) {
      this.config = config;
      this.testList = testList;
    }

    @Override
    public void run() throws Exception {
      testList.add(12);
      testList.add(23);
    }

    @Override
    public void stop() {
      testList.clear();
    }
  }

  private class TestPlatformBackgroundService extends PlatformBackgroundService {
    private Logger logger;
    private List<Integer> testList = new ArrayList<>();

    public TestPlatformBackgroundService(ConfigClient configClient, Logger logger) {
      super(configClient);
      this.logger = logger;
    }

    @Override
    protected PlatformBackgroundJob createBackgroundJob(Config config) {
      return new TestPlatformBackgroundJob(testList, config);
    }

    @Override
    protected Logger getLogger() {
      return this.logger;
    }
  }
}
