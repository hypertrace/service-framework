package org.hypertrace.core.serviceframework.http.guice;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleGuiceServletContextListener extends GuiceServletContextListener {
  private final Injector injector;

  @Override
  protected Injector getInjector() {
    return injector;
  }
}
