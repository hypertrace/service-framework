package org.hypertrace.core.serviceframework.background;

public interface PlatformBackgroundJob {
  void run() throws Exception;
  void stop();
}
