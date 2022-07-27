package org.hypertrace.core.serviceframework.http;

import java.util.List;

public interface HttpHandlerFactory {
  List<HttpHandlerDefinition> buildHandlers(HttpContainerEnvironment containerEnvironment);
}
