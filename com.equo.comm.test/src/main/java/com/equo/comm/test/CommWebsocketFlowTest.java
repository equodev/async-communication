package com.equo.comm.test;

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

import com.equo.chromium.swt.Browser;
import com.equo.comm.api.internal.IEventHandler;
import com.equo.comm.common.test.CommNormalFlow;

public class CommWebsocketFlowTest extends CommNormalFlow {

  private Integer port;

  protected int getWebsocketPort() {
    if (port == null) {
      ServiceReference<IEventHandler> svcref = context.getServiceReference(IEventHandler.class);
      Assert.assertNotNull(svcref);

      IEventHandler commService = context.getService(svcref);
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
    final Browser browser = (Browser) components.get(0);
    final Display display = Display.getDefault();
    display.syncExec(() -> {
      browser
          .setUrl("file://" + RESOURCES_DIR + resourcePath + "?equocommport=" + getWebsocketPort());
    });
  }

}
