package org.hypertrace.core.serviceframework.http;

import com.google.inject.Injector;
import java.util.List;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
public class HttpHandlerDefinition {
  String name;
  int port;
  String contextPath;
  Servlet servlet;
  int maxHeaderSizeBytes;
  CorsConfig corsConfig;
  Injector injector;
  MultipartConfigElement multipartConfig;

  @Accessors(fluent = true)
  boolean useSessions;

  @Value
  @Builder
  public static class CorsConfig {
    List<String> allowedHeaders;
    List<String> allowedOrigins;
  }
}
