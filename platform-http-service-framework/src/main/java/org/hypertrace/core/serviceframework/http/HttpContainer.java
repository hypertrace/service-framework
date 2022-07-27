package org.hypertrace.core.serviceframework.http;

public interface HttpContainer {
  void start();

  void blockUntilStopped();

  void stop();

  boolean isStopped();
}
