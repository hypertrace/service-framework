package org.hypertrace.core.serviceframework.hybrid;

import java.util.List;
import org.hypertrace.core.serviceframework.http.HttpHandlerDefinition;

@FunctionalInterface
public interface HybridHttpHandlerFactory {

  List<HttpHandlerDefinition> buildHandlers(HybridServiceContainerEnvironment containerEnvironment);
}
