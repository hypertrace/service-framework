package org.hypertrace.core.serviceframework.service.servlets;

import org.hypertrace.core.serviceframework.PlatformService;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HealthCheckServlet extends HttpServlet {

  private static final String PLAIN_TEXT_UTF_8 = "text/plain; charset=utf-8";

  PlatformService platformService;

  public HealthCheckServlet(PlatformService platformService) {
    this.platformService = platformService;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    final boolean b = platformService.healthCheck();
    int status = b ? 200 : 500;
    String respStr = b ? "OK" : " BAD";
    resp.setStatus(status);
    resp.setContentType(PLAIN_TEXT_UTF_8);
    resp.getOutputStream().print(respStr);
  }

}
