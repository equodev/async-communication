/****************************************************************************
**
** Copyright (C) 2021-2022 Equo
**
** This file is part of the Equo SDK.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equo.dev/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

package com.equo.comm.ws.provider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.equo.comm.api.actions.IActionHandler;
import com.equo.comm.api.annotations.EventName;
import com.equo.comm.api.error.CommMessageException;
import com.equo.comm.api.internal.EventMessage;
import com.equo.comm.api.internal.util.ActionHelper;
import com.equo.comm.ws.provider.entity.EventErrorMessage;
import com.equo.logging.client.api.Logger;
import com.equo.logging.client.api.LoggerFactory;
import com.google.gson.Gson;

/**
 * WebSocket server that relays messages to and from the event handler.
 */
@Component(service = EquoWebSocketServer.class, immediate = true)
public class EquoWebSocketServer extends WebSocketServer {
  protected static final Logger LOGGER = LoggerFactory.getLogger(EquoWebSocketServer.class);

  private boolean firstClientConnected = false;
  private List<String> messagesToSend = new ArrayList<>();

  private Map<String, Function<?, ?>> functionActionHandlers = new HashMap<>();
  private Map<String, Consumer<?>> consumerActionHandlers = new HashMap<>();
  private Map<String, Pair<?>> responseActionHandlers = new HashMap<>();
  private Map<String, Class<?>> actionParamTypes = new HashMap<>();

  private Gson gsonParser = new Gson();

  private volatile boolean started = false;

  public EquoWebSocketServer() {
    super(new InetSocketAddress(0));
  }

  /**
   * Starts websocket server when the service is activated.
   */
  @Activate
  public void activate() {
    LOGGER.info("Starting Equo websocket server...");
    start();
  }

  /**
   * Stops websocket server when the service is deactivated.
   */
  @Deactivate
  public void deactivate() {
    LOGGER.info("Stopping Equo websocket server...");
    try {
      stop();
    } catch (IOException | InterruptedException e) {
      // TODO: retry?
      e.printStackTrace();
    }
  }

  /**
   * Adds a Consumer event handler.
   * @param eventId       the action ID.
   * @param actionHandler the action handler.
   * @param paramTypes    types for the handler parameters.
   */
  public <T> void addEventHandler(String eventId, Consumer<T> actionHandler,
      Class<?>... paramTypes) {
    consumerActionHandlers.put(eventId, actionHandler);
    if (paramTypes != null && paramTypes.length == 1) {
      actionParamTypes.put(eventId, paramTypes[0]);
    }
  }

  /**
   * Adds a Function event handler.
   * @param eventId       the action ID.
   * @param actionHandler the action handler.
   * @param paramTypes    types for the handler parameters.
   */
  public <T, R> void addEventHandler(String eventId, Function<T, R> actionHandler,
      Class<?>... paramTypes) {
    functionActionHandlers.put(eventId, actionHandler);
    if (paramTypes != null && paramTypes.length == 1) {
      actionParamTypes.put(eventId, paramTypes[0]);
    }
  }

  /**
   * Adds a Function event handler.
   * @param eventId           the action ID.
   * @param responseTypeClass type of the expected response.
   */
  public <T> Future<T> addResponseHandler(String eventId, Class<T> responseTypeClass) {
    CompletableFuture<T> future = new CompletableFuture<T>();
    responseActionHandlers.put(eventId, new Pair<T>(future, responseTypeClass));
    return future;
  }

  /**
   * Removes an event handler.
   * @param actionId the action ID.
   */
  public void removeEventHandler(String actionId) {
    functionActionHandlers.remove(actionId);
    consumerActionHandlers.remove(actionId);
    actionParamTypes.remove(actionId);
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    LOGGER.debug(
        conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the Equo SDK!");
    this.firstClientConnected = true;
    synchronized (messagesToSend) {
      for (String messageToSend : messagesToSend) {
        broadcast(messageToSend);
      }
      messagesToSend.clear();
    }
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    LOGGER.debug(conn + " has left the Equo SDK!");
    broadcast(conn + " has left the Equo SDK!");
  }

  public void send(EventMessage eventMessage) {
    String messageAsJson = gsonParser.toJson(eventMessage);
    super.broadcast(messageAsJson);
  }

  /**
   * Called when a message arrives from javascript. Handles the message returning
   * a response if necessary.
   * @param message to receive from a handler.
   */
  @SuppressWarnings("unchecked")
  public void receiveMessage(String message, boolean broadcast) {
    EventMessage actionMessage = null;
    try {
      actionMessage = gsonParser.fromJson(message, EventMessage.class);
    } catch (Exception e) {
      // TODO: throw IllegalArgumentException
      return;
    }

    String actionId = actionMessage.getActionId();

    if (responseActionHandlers.containsKey(actionId)) {
      Pair<?> pair = responseActionHandlers.remove(actionId);
      CompletableFuture<Object> future = (CompletableFuture<Object>) pair.future;
      String messageError = actionMessage.getError();
      if (messageError != null) {
        future.completeExceptionally(new CommMessageException(-1, messageError));
      } else {
        future.complete(gsonParser.fromJson((String) actionMessage.getPayload(), pair.type));
      }
    }

    if (functionActionHandlers.containsKey(actionId)
        || consumerActionHandlers.containsKey(actionId)) {
      Object parsedPayload = null;
      if (actionMessage.getPayload() != null) {
        Class<?> type = actionParamTypes.get(actionId);
        String jsonString;
        if (actionMessage.getPayload() instanceof String) {
          jsonString = actionMessage.getPayload().toString();
        } else {
          jsonString = gsonParser.toJson(actionMessage.getPayload());
        }
        try {
          if (String.class.equals(type)) {
            parsedPayload = jsonString;
          } else {
            parsedPayload = gsonParser.fromJson(jsonString, type);
          }
        } catch (Exception e) {
          parsedPayload = jsonString;
        }
      }
      Function<?, ?> function = functionActionHandlers.get(actionId);
      Object response = null;
      try {
        if (function != null) {
          response = ((Function<Object, ?>) function).apply(parsedPayload);
        } else {
          Consumer<?> consumer = consumerActionHandlers.get(actionId);
          ((Consumer<Object>) consumer).accept(parsedPayload);
        }
      } catch (CommMessageException e) {
        final EventErrorMessage errorMessage = new EventErrorMessage(actionMessage.getCallbackId(),
            e.getErrorCode(), e.getLocalizedMessage());
        super.broadcast(gsonParser.toJson(errorMessage));
        return;
      }
      if (response != null) {
        EventMessage responseMessage = new EventMessage(actionMessage.getCallbackId(), response);
        super.broadcast(gsonParser.toJson(responseMessage));
      }
    }

  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    LOGGER.debug(conn + ": " + message);
    receiveMessage(message, true);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    LOGGER.debug(conn + ": " + message);
    broadcast(message.array());
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
    if (conn != null) {
      // some errors like port binding failed may not be assignable to a specific
      // websocket
    }
  }

  @Override
  public void onStart() {
    // TODO log web socket server started
    this.started = true;
    LOGGER.info("Equo Websocket Server started!");
  }

  @Override
  public void broadcast(String messageAsJson) {
    if (firstClientConnected) {
      super.broadcast(messageAsJson);
      receiveMessage(messageAsJson, false);
    } else {
      synchronized (messagesToSend) {
        messagesToSend.add(messageAsJson);
      }
    }
  }

  public boolean isStarted() {
    return started;
  }

  private static class Pair<T> {
    CompletableFuture<T> future;
    Class<T> type;

    Pair(CompletableFuture<T> future, Class<T> type) {
      this.future = future;
      this.type = type;
    }
  }

  /**
   * Method used to add all the Action Handler implementations.
   */
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setFunctionActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      final Class<?> parameterType;
      Type[] types = method.getGenericParameterTypes();
      if (types.length == 1) {
        parameterType = (Class<?>) types[0];
        actionParamTypes.put(actionHandlerName, parameterType);
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
              LOGGER.error("Error invoking action handler " + actionHandlerName, e2);
            }
          }
        };
        consumerActionHandlers.put(actionHandlerName, cons);
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
              LOGGER.error("Error invoking action handler " + actionHandlerName, e2);
              return null;
            }
          }
        };
        functionActionHandlers.put(actionHandlerName, func);
      }
    }
  }

  /**
   * Method to release all actions defined in this action handler.
   */
  public void unsetFunctionActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      functionActionHandlers.remove(actionHandlerName);
      consumerActionHandlers.remove(actionHandlerName);
      actionParamTypes.remove(actionHandlerName);
    }
  }

}
