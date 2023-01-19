package org.hypertrace.core.serviceframework;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.config.ConfigClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main class to launch any Platform service. */
public class PlatformServiceLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformServiceLauncher.class);
  private static PlatformService app;

  public static void main(String[] argv) {
    try {
      LOGGER.info("Trying to start PlatformService ...");
      PlatformServiceLauncher.updateRuntime();
      final ConfigClient configClient = ConfigClientFactory.getClient();
      PlatformService service = PlatformServiceFactory.get(configClient);
      PlatformServiceLauncher.setPlatFormServiceApp(service);
      service.initialize();
      service.start();
    } catch (Throwable e) {
      System.err.println("Got exception to start PlatformService.");
      e.printStackTrace();
    }
  }

  private static void updateRuntime() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    PlatformService platformService =
                        PlatformServiceLauncher.getPlatFormServiceApp();
                    if (platformService != null) {
                      platformService.shutdown();
                    }
                  } catch (Throwable e) {
                    System.err.println("Got exception while stopping PlatformService.");
                    e.printStackTrace();
                  } finally {
                    PlatformServiceLauncher.finalizeService();
                  }
                }));
  }

  private static PlatformService getPlatFormServiceApp() {
    return PlatformServiceLauncher.app;
  }

  private static void setPlatFormServiceApp(PlatformService service) {
    PlatformServiceLauncher.app = service;
  }

  private static void finalizeService() {
    String istioPilotQuitEndpoint = "http://127.0.0.1:15020/quitquitquit";
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = new HttpPost(istioPilotQuitEndpoint);
    try {
      httpclient.execute(httppost);
      LOGGER.debug("Request to pilot succeeded");
    } catch (Exception e) {
    }
  }
}
