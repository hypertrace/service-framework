package org.hypertrace.core.serviceframework.spi;

import java.util.concurrent.CompletionStage;

public interface PlatformServiceLifecycle {
  CompletionStage<Void> shutdownComplete();

  State getState();

  enum State {
    NOT_STARTED,
    INITIALIZING,
    INITIALIZED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED
  }
}
