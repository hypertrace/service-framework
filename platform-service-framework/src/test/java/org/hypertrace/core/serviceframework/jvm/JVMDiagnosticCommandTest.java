package org.hypertrace.core.serviceframework.jvm;

import java.lang.management.ManagementFactory;
import org.hypertrace.core.serviceframework.jvm.JVMDiagnosticCommand.Op;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JVMDiagnosticCommandTest {
  private JVMDiagnosticCommand command;

  @BeforeEach
  public void setup() {
    command = new JVMDiagnosticCommand(ManagementFactory.getPlatformMBeanServer());
  }

  @Test
  public void testHelpCommand() {
    String result = command.invoke(Op.HELP, Op.JVM_VERSION.getCmdlineArgs());
    Assertions.assertNotNull(result, "Command invocation failed");
  }

  @Test
  public void testJvmVersionCommand() {
    String result = command.invoke(Op.JVM_FLAGS, null);
    Assertions.assertNotNull(result, "Command invocation failed");
  }
}
