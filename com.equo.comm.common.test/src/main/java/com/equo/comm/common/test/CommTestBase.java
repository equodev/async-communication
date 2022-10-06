package com.equo.comm.common.test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.equo.chromium.swt.Browser;
import com.equo.comm.api.ICommService;
import com.equo.testing.common.osgi.base.BasicBrowserTest;

public class CommTestBase extends BasicBrowserTest {

  protected static final BundleContext context =
      FrameworkUtil.getBundle(CommTestBase.class).getBundleContext();

  protected static final String PWD = System.getProperty("user.dir") + "/";
  protected static final String RESOURCES_DIR = PWD + "src/main/resources/";

  private static ICommService getCommService() {
    ServiceReference<ICommService> svcref = context.getServiceReference(ICommService.class);
    assertNotNull(svcref);

    ICommService commService = context.getService(svcref);
    assertNotNull(commService);

    return commService;
  }

  protected static ICommService commService;

  protected void setFileResourceUrl(String resourcePath) {
    final Browser browser = (Browser) components.get(0);
    final Display display = Display.getDefault();
    display.syncExec(() -> {
      browser.setUrl("file://" + RESOURCES_DIR + resourcePath);
    });
  }

  @BeforeClass
  public static void initCommService() {
    commService = getCommService();
  }

  @Before
  public void waitForBrowser() {
    AtomicBoolean start = new AtomicBoolean(false);
    commService.on("_ready", runnable -> {
      start.set(true);
    });
    setFileResourceUrl("trigger_start.html");
    await().untilTrue(start);
  }

  protected static class TestPayload {
    private String id;
    private int number;
    private float fpNumber;
    private double dNumber;

    public TestPayload(String id, int number, float fpNumber, double dNumber) {
      this.id = id;
      this.number = number;
      this.fpNumber = fpNumber;
      this.dNumber = dNumber;
    }

    public double getdNumber() {
      return dNumber;
    }

    public void setdNumber(double dNumber) {
      this.dNumber = dNumber;
    }

    public float getFpNumber() {
      return fpNumber;
    }

    public void setFpNumber(float fpNumber) {
      this.fpNumber = fpNumber;
    }

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object otp) {
      if (!(otp instanceof TestPayload)) {
        return false;
      }
      TestPayload tp = (TestPayload) otp;
      return tp.id.equals(this.id) && tp.number == this.number && tp.fpNumber == this.fpNumber
          && tp.dNumber == this.dNumber;
    }
  }

  protected void testSimpleSend(String url, Object somePayload) {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceivePayload", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceivePayload", (payload) -> {
      // Try to fail faster than the timeout
      fail("Fail JSON");
    });
    commService.on("_startTransferReceivePayload", (payload) -> {
      assertNull(payload);
      commService.send("transfer", somePayload);
    });
    setFileResourceUrl(url);
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  protected void testSimpleReceive(String url, Object expectedPayload) {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("transferPayload", (payload) -> {
      assertNotNull(payload);
      assertEquals(expectedPayload, payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferSendPayload", (payload) -> {
      assertNull(payload);
      commService.send("transfer");
    });
    setFileResourceUrl(url);
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  protected void testSimpleResponse(String url, String somePayload) {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceiveResponse", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceiveResponse", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceiveResponse", (payload) -> {
      assertNull(payload);
      return somePayload;
    });
    setFileResourceUrl(url);
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

}
