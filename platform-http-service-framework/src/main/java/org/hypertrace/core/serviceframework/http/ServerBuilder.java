package org.hypertrace.core.serviceframework.http;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ServerBuilder<T extends ServerBuilder> {
  T addHandler(HttpHandlerDefinition handlerDefinition);

  T addHandlers(List<HttpHandlerDefinition> handlerDefinitions);

  T setExecutor(ExecutorService executorService);

  HttpContainer build();
}
