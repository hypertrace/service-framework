package org.hypertrace.core.serviceframework.http.jetty;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.hypertrace.core.serviceframework.http.HttpContainer;

@RequiredArgsConstructor
class JettyHttpContainer implements HttpContainer {
  private final Server server;
  private final ExecutorService executorService;
  private Future<?> future;

  @Override
  public void start() {
    this.future = this.executorService.submit(this::startAndWaitUnchecked);
  }

  @SneakyThrows
  @Override
  public void stop() {
    this.executorService.shutdown();
    this.executorService.awaitTermination(30, SECONDS);
  }

  @SneakyThrows
  @Override
  public void blockUntilStopped() {
    this.future.get();
  }

  @Override
  public boolean isStopped() {
    return this.server.isStopped();
  }

  @SneakyThrows
  private void startAndWaitUnchecked() {
    this.server.start();
    this.server.join();
  }
}
