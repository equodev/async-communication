package com.equo.comm.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.equo.comm.api.actions.IActionHandler;
import com.equo.comm.api.annotations.EventName;
import com.equo.comm.common.util.ActionHelper;
import com.equo.comm.common.util.Pair;

/**
 * Contains the action handlers.
 */
public class HandlerContainer {

  private Map<String, Function<?, ?>> functionActionHandlers = new ConcurrentHashMap<>();
  private Map<String, Consumer<?>> consumerActionHandlers = new ConcurrentHashMap<>();
  private Map<String, Class<?>> actionParamTypes = new ConcurrentHashMap<>();

  private Map<String, Pair<CompletableFuture<?>, Class<?>>> responseActionHandlers =
      new ConcurrentHashMap<>();

  private static HandlerContainer instance;

  /**
   * Returns the HandlerContainer singleton.
   * @return singleton instance
   */
  public static HandlerContainer getInstance() {
    if (instance == null) {
      instance = new HandlerContainer();
    }
    return instance;
  }

  private HandlerContainer() {
  }

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

  /**
   * Adds all action handlers associated with the parameter.
   */
  public void addActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      final Class<?> parameterType;
      Type[] types = method.getGenericParameterTypes();
      if (types.length == 1) {
        parameterType = (Class<?>) types[0];
        putActionParamType(actionHandlerName, parameterType);
      } else {
        parameterType = Object.class;
      }
      Class<?> rt = method.getReturnType();
      if (Void.TYPE.equals(rt)) {
        Consumer<?> cons = (param) -> {
          try {
            method.invoke(actionHandler, parameterType.cast(param));
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
              | ClassCastException e1) {
            try {
              method.invoke(actionHandler);
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e2) {
              return;
            }
          }
        };
        putConsumer(actionHandlerName, cons);
      } else {
        Function<?, ?> func = (param) -> {
          try {
            return method.invoke(actionHandler, parameterType.cast(param));
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
              | ClassCastException e1) {
            try {
              return method.invoke(actionHandler);
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e2) {
              return null;
            }
          }
        };
        putFunction(actionHandlerName, func);
      }
    }
  }

  /**
   * Removes all action handlers associated with the parameter.
   */
  public void removeActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      removeFunction(actionHandlerName);
      removeConsumer(actionHandlerName);
      removeActionParamType(actionHandlerName);
    }
  }

}
