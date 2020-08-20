package org.hypertrace.core.serviceframework.background;

import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import com.typesafe.config.Config;
import org.slf4j.Logger;

/**
 * Abstract class for long running background services usually that consume from a Kafka topic and produce into
 * another. The workhorse is wrapped in an implementation of PlatformBackgroundJob.
 */
public abstract class PlatformBackgroundService extends PlatformService {

  private PlatformBackgroundJob job;

  public PlatformBackgroundService(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {


    try {
      this.job = createBackgroundJob(getAppConfig());
    } catch (Exception e) {
      getLogger().error("Failed to initialize BackgroundJob.", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doStart() {
    try {
      job.run();
    } catch (Exception e) {
      getLogger().error("Got exception while running BackgroundJob", e);
      e.printStackTrace();
      // Since event compute job couldn't recover from the error state like this.
      // It's better to kill the entire process and let the guardian process to restart the job,
      // e.g. systemd or kubernetes.
      System.exit(1);
    }
  }

  @Override
  protected void doStop() {
    job.stop();
  }

  @Override
  public boolean healthCheck() {
    return true;
  }

  protected abstract PlatformBackgroundJob createBackgroundJob(Config config);

  protected abstract Logger getLogger();
}
