package com.equo.comm.common.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.equo.comm.api.ICommSendService;
import com.equo.comm.common.util.Pair;

/**
 * NoOp implementation for send events.
 */
public class NoOpSendEventHandler implements ICommSendService {

  private List<Pair<Pair<String, Object>, Pair<CompletableFuture<?>, Class<?>>>> sendEvents =
      new ArrayList<>();
  private List<CompletableFuture<?>> pendingFutures = new ArrayList<>();

  /**
   * Sends all pending events through the specified send event handler.
   */
  @SuppressWarnings("unchecked")
  public void flush(ICommSendService service) {
    for (Pair<Pair<String, Object>, Pair<CompletableFuture<?>, Class<?>>> sendEvent : sendEvents) {
      Pair<String, Object> userEvent = sendEvent.getFirst();
      String userEventId = userEvent.getFirst();
      Object payload = userEvent.getSecond();
      Pair<CompletableFuture<?>, Class<?>> response = sendEvent.getSecond();
      if (response != null) {
        final CompletableFuture<Object> future = (CompletableFuture<Object>) response.getFirst();
        Class<?> responseTypeClass = response.getSecond();
        pendingFutures.add(future);
        final CompletableFuture<Object> actualFuture =
            (CompletableFuture<Object>) service.send(userEventId, payload, responseTypeClass);
        actualFuture.thenAccept((actualResponse) -> {
          future.complete(actualResponse);
          pendingFutures.remove(future);
        });
        actualFuture.exceptionally((throwable) -> {
          future.completeExceptionally(throwable);
          pendingFutures.remove(future);
          return null;
        });
      } else {
        service.send(userEventId, payload);
      }
    }
    sendEvents.clear();
  }

  @Override
  public void send(String userEvent, Object payload) {
    Pair<Pair<String, Object>, Pair<CompletableFuture<?>, Class<?>>> sendEvent =
        new Pair<>(new Pair<>(userEvent, payload), null);
    sendEvents.add(sendEvent);
  }

  @Override
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass) {
    CompletableFuture<T> future = new CompletableFuture<>();
    Pair<Pair<String, Object>, Pair<CompletableFuture<?>, Class<?>>> sendEvent =
        new Pair<>(new Pair<>(userEvent, payload), new Pair<>(future, responseTypeClass));
    sendEvents.add(sendEvent);
    return future;
  }

  @Override
  public void send(String userEvent) {
    send(userEvent, (Object) null);
  }

  @Override
  public <T> Future<T> send(String userEvent, Class<T> responseTypeClass) {
    return send(userEvent, null, responseTypeClass);
  }

}
