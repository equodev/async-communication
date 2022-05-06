package com.equo.comm.common.test;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.equo.chromium.swt.Browser;
import com.equo.comm.api.ICommService;
import com.equo.comm.api.error.CommMessageException;
import com.equo.testing.common.osgi.base.BasicBrowserTest;

public class CommNormalFlow extends BasicBrowserTest {

  protected static final BundleContext context =
      FrameworkUtil.getBundle(CommNormalFlow.class).getBundleContext();

  protected static final String PWD = System.getProperty("user.dir") + "/";
  protected static final String RESOURCES_DIR = PWD + "src/main/resources/";

  protected static ICommService getCommService() {
    ServiceReference<ICommService> svcref = context.getServiceReference(ICommService.class);
    Assert.assertNotNull(svcref);

    ICommService commService = context.getService(svcref);
    Assert.assertNotNull(commService);

    return commService;
  }

  private static ICommService commService;

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
    setResourceUrl("trigger_start.html");
    Awaitility.await().untilTrue(start);
  }

  protected void setResourceUrl(String resourcePath) {
    final Browser browser = (Browser) components.get(0);
    final Display display = Display.getDefault();
    display.syncExec(() -> {
      browser.setUrl("file://" + RESOURCES_DIR + resourcePath);
    });
  }

  @Test
  public void canTransferMessagesToAndFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferNoPayload", (payload) -> {
      Assert.assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferNoPayload", (payload) -> {
      Assert.assertNull(payload);
      commService.send("transfer");
    });
    setResourceUrl("basic-test/no-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(25)).untilTrue(success);
  }

  protected static class TestPayload {
    private String id;
    private int number;
    private float fpNumber;
    private double dNumber;

    TestPayload(String id, int number, float fpNumber, double dNumber) {
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

  @Test
  public void canReceivePayloadsFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    TestPayload expectedPayload = new TestPayload("success", 2, 3.5F, 5.0);
    commService.on("transferPayload", TestPayload.class, (payload) -> {
      Assert.assertNotNull(payload);
      Assert.assertEquals(expectedPayload, payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferSendPayload", (payload) -> {
      Assert.assertNull(payload);
      commService.send("transfer");
    });
    setResourceUrl("basic-test/send-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

  @Test
  public void canSendPayloadsToJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceivePayload", (payload) -> {
      Assert.assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceivePayload", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceivePayload", (payload) -> {
      Assert.assertNull(payload);
      TestPayload somePayload = new TestPayload("success", 2, 3.5F, 5.0);
      commService.send("transfer", somePayload);
    });
    setResourceUrl("basic-test/receive-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

  @Test
  public void canReceiveResponsesFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    TestPayload expectedPayload = new TestPayload("success", 2, 3.5F, 5.0);
    commService.on("_startTransferSendResponse", (payload) -> {
      Assert.assertNull(payload);
      Future<TestPayload> maybeResponse = commService.send("transfer", TestPayload.class);
      Assert.assertEquals(CompletableFuture.class, maybeResponse.getClass());
      CompletableFuture<TestPayload> maybeCompletableResponse =
          (CompletableFuture<TestPayload>) maybeResponse;
      maybeCompletableResponse.thenAccept((somePayload) -> {
        Assert.assertNotNull(somePayload);
        Assert.assertEquals(TestPayload.class, somePayload.getClass());
        Assert.assertEquals(expectedPayload, somePayload);
        success.compareAndSet(false, true);
      });
    });
    setResourceUrl("basic-test/send-response.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

  @Test
  public void canRespondToJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceiveResponse", (payload) -> {
      Assert.assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceiveResponse", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceiveResponse", TestPayload.class, (payload) -> {
      Assert.assertNull(payload);
      TestPayload somePayload = new TestPayload("success", 2, 3.5F, 5.0);
      return somePayload;
    });
    setResourceUrl("basic-test/receive-response.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

  @Test
  public void javascriptReceivesHandlerErrors() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceiveError", (payload) -> {
      Assert.assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceiveError", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceiveError", (Consumer<String>) (payload) -> {
      Assert.assertNull(payload);
      throw new CommMessageException(5, "some message");
    });
    setResourceUrl("error-test/receive-error.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

  @Test
  public void javaReceivesHandlerErrors() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("_startTransferSendError", (Consumer<String>) (payload) -> {
      Assert.assertNull(payload);
      CompletableFuture<TestPayload> future =
          (CompletableFuture<TestPayload>) commService.send("transfer", TestPayload.class);
      future.handle((pool, jsException) -> {
        Assert.assertNotNull(jsException);
        Assert.assertEquals(CommMessageException.class, jsException.getClass());
        CommMessageException commException = (CommMessageException) jsException;
        Assert.assertEquals(-1, commException.getErrorCode());
        Assert.assertEquals("some message", commException.getMessage());
        success.compareAndSet(false, true);
        return null;
      });
    });
    setResourceUrl("error-test/send-error.html");
    Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
  }

}
