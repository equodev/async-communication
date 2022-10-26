package com.equo.comm.common.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.Test;

import com.equo.comm.api.error.CommMessageException;

public class CommNormalFlow extends CommTestBase {

  @Test
  public void canTransferMessagesToAndFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferNoPayload", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferNoPayload", (payload) -> {
      assertNull(payload);
      commService.send("transfer");
    });
    setFileResourceUrl("basic-test/no-payload.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void canReceivePayloadsFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    TestPayload expectedPayload = new TestPayload("success", 2, 3.5F, 5.0);
    commService.on("transferPayload", TestPayload.class, (payload) -> {
      assertNotNull(payload);
      assertEquals(expectedPayload, payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferSendPayload", (payload) -> {
      assertNull(payload);
      commService.send("transfer");
    });
    setFileResourceUrl("basic-test/send-payload.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void canSendPayloadsToJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceivePayload", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceivePayload", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceivePayload", (payload) -> {
      assertNull(payload);
      TestPayload somePayload = new TestPayload("success", 2, 3.5F, 5.0);
      commService.send("transfer", somePayload);
    });
    setFileResourceUrl("basic-test/receive-payload.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void canReceiveResponsesFromJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    TestPayload expectedPayload = new TestPayload("success", 2, 3.5F, 5.0);
    commService.on("_startTransferSendResponse", (payload) -> {
      assertNull(payload);
      Future<TestPayload> maybeResponse = commService.send("transfer", TestPayload.class);
      assertEquals(CompletableFuture.class, maybeResponse.getClass());
      CompletableFuture<TestPayload> maybeCompletableResponse =
          (CompletableFuture<TestPayload>) maybeResponse;
      maybeCompletableResponse.thenAccept((somePayload) -> {
        assertNotNull(somePayload);
        assertEquals(TestPayload.class, somePayload.getClass());
        assertEquals(expectedPayload, somePayload);
        success.compareAndSet(false, true);
      });
    });
    setFileResourceUrl("basic-test/send-response.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void canRespondToJavaScript() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceiveResponse", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceiveResponse", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceiveResponse", TestPayload.class, (payload) -> {
      assertNull(payload);
      TestPayload somePayload = new TestPayload("success", 2, 3.5F, 5.0);
      return somePayload;
    });
    setFileResourceUrl("basic-test/receive-response.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void javascriptReceivesHandlerErrors() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferReceiveError", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("failTransferReceiveError", (payload) -> {
      // Try to fail faster than the timeout
      fail();
    });
    commService.on("_startTransferReceiveError", (Consumer<String>) (payload) -> {
      assertNull(payload);
      throw new CommMessageException(5, "some message");
    });
    setFileResourceUrl("error-test/receive-error.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void javaReceivesHandlerErrors() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("_startTransferSendError", (Consumer<String>) (payload) -> {
      assertNull(payload);
      CompletableFuture<TestPayload> future =
          (CompletableFuture<TestPayload>) commService.send("transfer", TestPayload.class);
      future.handle((pool, jsException) -> {
        assertNotNull(jsException);
        assertEquals(CommMessageException.class, jsException.getClass());
        CommMessageException commException = (CommMessageException) jsException;
        assertEquals(-1, commException.getErrorCode());
        assertEquals("some message", commException.getMessage());
        success.compareAndSet(false, true);
        return null;
      });
    });
    setFileResourceUrl("error-test/send-error.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void shouldCompleteFutureExceptionallyWhenThereIsNoJavascriptHandler() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("_startTransferReceivePayload", (payload) -> {
      assertThat(payload).isNull();
      CompletableFuture<String> future =
          (CompletableFuture<String>) commService.send("transfer", null, String.class);
      future.exceptionally(throwable -> {
        assertThat(throwable.getMessage())
            .isEqualTo("An event handler does not exist for the user event 'transfer'");
        assertThat(throwable.getClass()).isEqualTo(CommMessageException.class);
        assertThat(((CommMessageException) throwable).getErrorCode()).isEqualTo(255);
        success.compareAndSet(false, true);
        return null;
      });
    });
    setFileResourceUrl("error-test/receive-payload-non-existent-user-event.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void shouldRejectPromiseWhenThereIsNoJavaHandler() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("completeTest", error -> {
      assertThat(error).isEqualTo(
          "{\"code\":255.0,\"message\":\"An event handler does not exist for the user event \\u0027eventShouldNotExist\\u0027\"}");
      success.compareAndSet(false, true);
    });
    setFileResourceUrl("error-test/send-payload-non-existent-user-event.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

  @Test
  public void canDeclareHandlersAsOsgiComponents() {
    AtomicBoolean success = new AtomicBoolean(false);
    commService.on("successTransferComponentActionHandler", (payload) -> {
      assertNull(payload);
      success.compareAndSet(false, true);
    });
    commService.on("_startTransferComponentActionHandler", (payload) -> {
      assertNull(payload);
      commService.send("transfer");
    });
    setFileResourceUrl("basic-test/send-payload-component-action-handler.html");
    await().timeout(Duration.ofSeconds(3)).untilTrue(success);
  }

}
