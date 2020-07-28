package org.hypertrace.core.serviceframework.service.servlets;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.HttpTester.Request;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JVMDiagnosticServletTest {

  private static ServletTester server;

  @BeforeAll
  public static void setup() throws Exception {
    server = new ServletTester();
    server.setContextPath("/");
    server.addServlet(JVMDiagnosticServlet.class, "/diags/*");
    server.start();
  }

  @AfterAll
  public static void teardown() throws Exception {
    server.stop();
  }

  @Test
  public void testHelpCommand() throws Exception {
    final Response response = processRequest("/diags/help");

    Assertions.assertEquals(HttpStatus.OK_200, response.getStatus());
    Assertions.assertNotNull(response.getContent());
  }

  @Test
  public void testJVMVersionCommand() throws Exception {
    final Response response = processRequest("/diags/jvm/version");

    Assertions.assertEquals(HttpStatus.OK_200, response.getStatus());
    Assertions.assertNotNull(response.getContent());
    Assertions.assertTrue(response.getContent().contains("version"),
        "Response doesn't contain word 'version'. Response received: " + response.getContent());
  }

  @Test
  public void testUnknownCommand() throws Exception {
    final Response response = processRequest("/diags/unknown");

    // Unknown command returns 200 but with with error message
    Assertions.assertEquals(HttpStatus.OK_200, response.getStatus());
    Assertions.assertTrue(response.getContent().startsWith("Error"),
        "Response expected to contain error message. Response received: " + response.getContent());

  }

  private Response processRequest(String uri) throws Exception {
    final Request req = HttpTester.newRequest();
    req.setHeader("Host", "tester");
    req.startRequest("GET", uri, HttpVersion.HTTP_1_1);

    return HttpTester.parseResponse(server.getResponses(req.generate()));
  }
}
