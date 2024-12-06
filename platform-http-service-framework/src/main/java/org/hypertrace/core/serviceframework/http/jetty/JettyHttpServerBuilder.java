package org.hypertrace.core.serviceframework.http.jetty;

import static com.google.common.base.Joiner.on;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContextListener;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.hypertrace.core.serviceframework.http.HttpContainer;
import org.hypertrace.core.serviceframework.http.HttpHandlerDefinition;
import org.hypertrace.core.serviceframework.http.HttpHandlerDefinition.CorsConfig;
import org.hypertrace.core.serviceframework.http.ServerBuilder;
import org.hypertrace.core.serviceframework.http.guice.SimpleGuiceServletContextListener;

public class JettyHttpServerBuilder implements ServerBuilder<JettyHttpServerBuilder> {
  private final List<HttpHandlerDefinition> handlers = new LinkedList<>();
  @Nullable private ExecutorService executorService;

  @Override
  public JettyHttpServerBuilder addHandler(HttpHandlerDefinition handlerDefinition) {
    this.handlers.add(handlerDefinition);
    return this;
  }

  @Override
  public JettyHttpServerBuilder addHandlers(List<HttpHandlerDefinition> handlerDefinitions) {
    handlerDefinitions.forEach(this::addHandler);
    return this;
  }

  @Override
  public JettyHttpServerBuilder setExecutor(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  @Override
  public HttpContainer build() {
    Server server = new Server();
    this.handlers.stream()
        .map(
            (HttpHandlerDefinition definition) -> this.buildConnectorForHandler(server, definition))
        .forEach(server::addConnector);

    server.setHandler(this.buildCompositeHandler(this.handlers));
    server.setStopAtShutdown(true);
    return new JettyHttpContainer(
        server,
        Optional.ofNullable(this.executorService).orElseGet(Executors::newSingleThreadExecutor));
  }

  private Connector buildConnectorForHandler(
      Server server, HttpHandlerDefinition handlerDefinition) {
    ServerConnector connector =
        new ServerConnector(server, this.buildConnectionFactory(handlerDefinition));
    connector.setPort(handlerDefinition.getPort());
    connector.setName(handlerDefinition.getName());
    return connector;
  }

  private HttpConnectionFactory buildConnectionFactory(HttpHandlerDefinition handlerDefinition) {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(false);
    if (handlerDefinition.getMaxHeaderSizeBytes() > 0) {
      httpConfig.setRequestHeaderSize(handlerDefinition.getMaxHeaderSizeBytes());
    }
    return new HttpConnectionFactory(httpConfig);
  }

  private Handler buildCompositeHandler(List<HttpHandlerDefinition> handlerDefinitions) {
    ContextHandlerCollection compositeHandler = new ContextHandlerCollection();

    handlerDefinitions.stream().map(this::buildHandler).forEach(compositeHandler::addHandler);

    return compositeHandler;
  }

  private Handler buildHandler(HttpHandlerDefinition handlerDefinition) {
    int options =
        handlerDefinition.useSessions()
            ? ServletContextHandler.SESSIONS
            : ServletContextHandler.NO_SESSIONS;
    ServletContextHandler context = new ServletContextHandler(options);
    ErrorHandler errorHandler = new ErrorPageErrorHandler();
    errorHandler.setShowServlet(false);
    errorHandler.setShowStacks(false);
    context.setErrorHandler(errorHandler);
    this.buildCorsFilterIfRequired(handlerDefinition.getCorsConfig())
        .ifPresent(
            corsFilter ->
                context.addFilter(
                    corsFilter,
                    this.wildcardSubpath(handlerDefinition.getContextPath()),
                    EnumSet.of(DispatcherType.REQUEST)));
    this.buildGuiceFilterIfRequired(handlerDefinition.getInjector())
        .ifPresent(
            guiceFilter ->
                context.addFilter(
                    guiceFilter,
                    this.wildcardSubpath(handlerDefinition.getContextPath()),
                    EnumSet.of(DispatcherType.REQUEST)));
    this.buildGuiceContextListenerIfRequired(handlerDefinition.getInjector())
        .ifPresent(context::addEventListener);
    this.buildServletHolderIfRequired(handlerDefinition)
        .ifPresent(
            servletHolder -> context.addServlet(servletHolder, handlerDefinition.getContextPath()));
    context.setVirtualHosts(new String[] {"@" + handlerDefinition.getName()});
    return context;
  }

  private Optional<ServletHolder> buildServletHolderIfRequired(
      HttpHandlerDefinition handlerDefinition) {
    if (isNull(handlerDefinition.getServlet())) {
      return Optional.empty();
    }
    ServletHolder servletHolder = new ServletHolder(handlerDefinition.getServlet());
    Optional.of(handlerDefinition.getServletInitParameters())
        .orElse(Map.of())
        .forEach(servletHolder::setInitParameter);
    Optional.ofNullable(handlerDefinition.getMultipartConfig())
        .ifPresent(servletHolder.getRegistration()::setMultipartConfig);
    return Optional.of(servletHolder);
  }

  private Optional<FilterHolder> buildCorsFilterIfRequired(@Nullable CorsConfig config) {
    if (isNull(config)) {
      return Optional.empty();
    }
    FilterHolder crossOriginFilterHolder = new FilterHolder(CrossOriginFilter.class);
    ofNullable(config.getAllowedOrigins())
        .map(on(",")::join)
        .ifPresent(
            origins ->
                crossOriginFilterHolder.setInitParameter(
                    CrossOriginFilter.ALLOWED_ORIGINS_PARAM, origins));
    ofNullable(config.getAllowedHeaders())
        .map(on(",")::join)
        .ifPresent(
            headers ->
                crossOriginFilterHolder.setInitParameter(
                    CrossOriginFilter.ALLOWED_HEADERS_PARAM, headers));
    return Optional.of(crossOriginFilterHolder);
  }

  private Optional<FilterHolder> buildGuiceFilterIfRequired(@Nullable Injector injector) {
    if (isNull(injector)) {
      return Optional.empty();
    }
    return Optional.of(new FilterHolder(GuiceFilter.class));
  }

  private Optional<ServletContextListener> buildGuiceContextListenerIfRequired(
      @Nullable Injector injector) {
    if (isNull(injector)) {
      return Optional.empty();
    }
    return Optional.of(new SimpleGuiceServletContextListener(injector));
  }

  private String wildcardSubpath(String path) {
    return Path.of(path, "*").toString();
  }
}
