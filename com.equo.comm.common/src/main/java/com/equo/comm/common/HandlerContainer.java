package com.equo.comm.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;

import com.equo.comm.common.util.Pair;

/**
 * Contains the action handlers.
 */
@Component(service = HandlerContainer.class)
public class HandlerContainer {

  private Map<String, Function<?, ?>> functionActionHandlers = new HashMap<>();
  private Map<String, Consumer<?>> consumerActionHandlers = new HashMap<>();
  private Map<String, Class<?>> actionParamTypes = new HashMap<>();

  private Map<String, Pair<CompletableFuture<?>, Class<?>>> responseActionHandlers =
      new HashMap<>();

  public void putResponse(String responseId, Pair<CompletableFuture<?>, Class<?>> value) {
    responseActionHandlers.put(responseId, value);
  }

  public Pair<CompletableFuture<?>, Class<?>> getResponse(String responseId) {
    return responseActionHandlers.remove(responseId);
  }

  public void putConsumer(String key, Consumer<?> value) {
    consumerActionHandlers.put(key, value);
  }

  public void removeConsumer(String key) {
    consumerActionHandlers.remove(key);
  }

  public Consumer<?> getConsumer(String key) {
    return consumerActionHandlers.get(key);
  }

  public void putFunction(String key, Function<?, ?> value) {
    functionActionHandlers.put(key, value);
  }

  public void removeFunction(String key) {
    functionActionHandlers.remove(key);
  }

  public Function<?, ?> getFunction(String key) {
    return functionActionHandlers.get(key);
  }

  public void putActionParamType(String key, Class<?> value) {
    actionParamTypes.put(key, value);
  }

  public void removeActionParamType(String key) {
    actionParamTypes.remove(key);
  }

  public Class<?> getActionParamType(String key) {
    return actionParamTypes.get(key);
  }

  /**
   * Removes the actionId from all containers.
   * @param actionId to remove
   */
  public void removeEventHandler(String actionId) {
    removeFunction(actionId);
    removeConsumer(actionId);
    removeActionParamType(actionId);
  }

}
