package org.hypertrace.core.serviceframework.jvm;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Provide virtual machine diagnostics using DiagnosticCommandMBean that comes with the Oracle
 * HotSpot JVM.
 */
public class JVMDiagnosticCommand {

  /** Object Name of DiagnosticCommandMBean. */
  public static final String DIAGNOSTIC_COMMAND_MBEAN_OBJECT_NAME =
      "com.sun.management:type=DiagnosticCommand";

  private final MBeanServer server;
  private final ObjectName objectName;

  /**
   * Create an instance with the provided object name.
   *
   * @param server ObjectName associated with DiagnosticCommand MBean.
   */
  public JVMDiagnosticCommand(MBeanServer server) {
    this.server = server;
    try {
      this.objectName = new ObjectName(DIAGNOSTIC_COMMAND_MBEAN_OBJECT_NAME);
    } catch (Exception e) {
      throw new RuntimeException("Unable to create an ObjectName", e);
    }
  }

  /**
   * Invoke operation on the DiagnosticCommandMBean that accepts String array argument and returns a
   * String.
   *
   * @param operation DiagnosticCommandMBean Operation to run.
   * @return String returned by DiagnosticCommandMBean operation.
   */
  public String invoke(final Op operation, final String[] args) {
    String result;
    try {
      result =
          (String)
              server.invoke(
                  objectName,
                  operation.apiOpName,
                  new Object[] {args},
                  new String[] {String[].class.getName()});
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return result;
  }

  /**
   * Maintains the mapping between command-line and api operation names. If needed, this can be
   * extended as well for other operation attributes like args types.
   */
  public enum Op {
    JVM_THREADS("threadPrint", "Thread.Print"),
    JVM_THREADS_DUMP_TO_FILE("threadDumpToFile", "Thread.dump_to_file", "/tmp/jvm_thread_dump"),
    JVM_CLASSLOADERS("vmClassloaders", "VM.classloaders"),
    JVM_CLASSLOADER_STATS("vmClassloaderStats", "VM.classloader_stats"),
    JVM_CMDLINE("vmCommandLine", "VM.command_line"),
    JVM_DYNAMIC_LIBS("vmDynlibs", "VM.dynlibs"),
    JVM_FLAGS("vmFlags", "VM.flags"),
    JVM_INFO("vmInfo", "VM.info"),
    JVM_METASPACE("vmMetaspace", "VM.metaspace"),
    JVM_NATIVE_MEMORY("vmNativeMemory", "VM.native_memory"),
    JVM_VERSION("vmVersion", "VM.version"),
    JVM_UPTIME("vmUptime", "VM.uptime"),
    GC_CLASS_HISTO("gcClassHistogram", "GC.class_histogram"),
    GC_HEAP_INFO("gcHeapInfo", "GC.heap_info"),
    HELP("help", "help");

    private final String apiOpName;
    private final String[] cmdlineArgs;

    Op(String apiOpName, String... cmdlineArgs) {
      this.apiOpName = apiOpName;
      this.cmdlineArgs = cmdlineArgs;
    }

    public String getApiOpName() {
      return apiOpName;
    }

    public String[] getCmdlineArgs() {
      return cmdlineArgs;
    }
  }
}
