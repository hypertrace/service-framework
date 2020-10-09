package org.hypertrace.core.serviceframework;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class DefaultPlatformServiceLifecycle implements PlatformServiceLifecycle {
  private volatile State state = State.NOT_STARTED;
  private final CompletableFuture<Void> shutdown = new CompletableFuture<>();

  @Override
  public CompletionStage<Void> shutdownComplete() {
    return this.shutdown.minimalCompletionStage();
  }

  @Override
  public State getState() {
    return this.state;
  }

  void setState(State state) {
    this.state = state;
    if (this.state == State.STOPPED) {
      this.shutdown.complete(null);
    }
  }
}
