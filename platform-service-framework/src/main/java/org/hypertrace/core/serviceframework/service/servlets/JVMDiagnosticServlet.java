package org.hypertrace.core.serviceframework.service.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hypertrace.core.serviceframework.jvm.JVMDiagnosticCommand;
import org.hypertrace.core.serviceframework.jvm.JVMDiagnosticCommand.Op;

/**
 * Servlet to get the JVM diagnostics. Usage documentation for all the operation available under
 * '/diags/help'
 */
public class JVMDiagnosticServlet extends HttpServlet {
  private static final long serialVersionUID = 4933449066579320238L;
  private static final String CONTENT_TYPE = "text/plain";
  private transient JVMDiagnosticCommand command;

  private static final Map<String, Op> URI_TO_OP = new HashMap<>();

  static {
    URI_TO_OP.put("/jvm/threads", Op.JVM_THREADS);
    URI_TO_OP.put("/jvm/classloaders", Op.JVM_CLASSLOADERS);
    URI_TO_OP.put("/jvm/classloader-stats", Op.JVM_CLASSLOADER_STATS);
    URI_TO_OP.put("/jvm/cmdline", Op.JVM_CMDLINE);
    URI_TO_OP.put("/jvm/dynlibs", Op.JVM_DYNAMIC_LIBS);
    URI_TO_OP.put("/jvm/info", Op.JVM_INFO);
    URI_TO_OP.put("/jvm/metaspace", Op.JVM_METASPACE);
    URI_TO_OP.put("/jvm/native-mem", Op.JVM_NATIVE_MEMORY);
    URI_TO_OP.put("/jvm/flags", Op.JVM_FLAGS);
    URI_TO_OP.put("/jvm/version", Op.JVM_VERSION);
    URI_TO_OP.put("/jvm/uptime", Op.JVM_UPTIME);
    URI_TO_OP.put("/gc/histo", Op.GC_CLASS_HISTO);
    URI_TO_OP.put("/gc/heapinfo", Op.GC_HEAP_INFO);
    URI_TO_OP.put("/help", Op.HELP);
  }

  @Override
  public void init() throws ServletException {
    try {
      // Some JVMs doesn't allow java.lang.managament
      this.command = new JVMDiagnosticCommand(ManagementFactory.getPlatformMBeanServer());
    } catch (NoClassDefFoundError ncdfe) {
      // we won't be able to provide JVM diagnostics
      this.command = null;
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType(CONTENT_TYPE);
    resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

    final PrintWriter responseWriter = resp.getWriter();
    if (command == null) {
      responseWriter
          .println("Error: runtime environment does not allow to run diagnostic commands.");
      return;
    }

    String uri = req.getPathInfo();
    Op operation = URI_TO_OP.get(uri);
    if (operation == null) {
      responseWriter.println("Error: Operation not found for uri:" + uri);
      return;
    }

    if (operation == Op.HELP) {
      processHelpCommand(req, responseWriter);
      return;
    }

    final String[] args = req.getParameterValues("args");
    processOp(operation, args, responseWriter);
  }

  private void processHelpCommand(HttpServletRequest req,
      PrintWriter responseWriter) {
    for (Entry<String, Op> entry : URI_TO_OP.entrySet()) {
      String relativePath = entry.getKey();
      Op op = entry.getValue();

      if (op == Op.HELP) {
        continue;
      }
      responseWriter.println("=================================================================");
      responseWriter.println("URI: " + req.getServletPath() + relativePath);
      responseWriter.println("=================================================================");
      processOp(Op.HELP, new String[]{op.getCmdlineOpName()}, responseWriter);
    }
  }

  private void processOp(Op operation, String[] args, PrintWriter responseWriter) {
    try {
      String result = command.invoke(operation, args);
      responseWriter.println(result);
    } catch (Exception e) {
      responseWriter.println("Error occurred while processing: '" + operation.getApiOpName() + "'");
      e.printStackTrace(responseWriter);
    }
  }
}
