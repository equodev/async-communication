package com.equo.comm.test;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.osgi.framework.ServiceReference;

import com.equo.comm.api.ICommService;
import com.equo.comm.common.test.CommNormalFlow;

public class CommWebsocketFlowTest extends CommNormalFlow {

  private Integer port;

  protected int getWebsocketPort() {
    if (port == null) {
      ServiceReference<ICommService> svcref = context.getServiceReference(ICommService.class);
      Assert.assertNotNull(svcref);

      ICommService commService = context.getService(svcref);
      Assert.assertNotNull(commService);

      Class<?> serviceClass = commService.getClass();
      Method getPort;
      try {
        getPort = serviceClass.getDeclaredMethod("getPort", (Class<?>[]) null);
        port = Integer.valueOf((int) getPort.invoke(commService, (Object[]) null));
      } catch (Exception e) {
        throw new RuntimeException();
      }
    }
    return port.intValue();
  };

  @Override
  protected void setFileResourceUrl(String resourcePath) {
    uiDispatch.syncExec(() -> {
      currentBrowser
          .setUrl("file://" + RESOURCES_DIR + resourcePath + "?equoCommPort=" + getWebsocketPort());
    });
  }

}
