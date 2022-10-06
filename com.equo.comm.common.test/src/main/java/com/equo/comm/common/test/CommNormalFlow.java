package com.equo.comm.common.test;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

import com.equo.comm.api.error.CommMessageException;

public class CommNormalFlow extends CommTestBase {

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
    setFileResourceUrl("basic-test/no-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("basic-test/send-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("basic-test/receive-payload.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("basic-test/send-response.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("basic-test/receive-response.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("error-test/receive-error.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
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
    setFileResourceUrl("error-test/send-error.html");
    Awaitility.await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

}
