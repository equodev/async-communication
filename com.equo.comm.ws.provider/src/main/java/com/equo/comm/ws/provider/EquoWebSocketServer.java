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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.equo.comm.api.error.CommMessageException;
import com.equo.comm.common.HandlerContainer;
import com.equo.comm.common.MessageHandler;
import com.equo.comm.common.entity.EventErrorMessage;
import com.equo.comm.common.entity.EventMessage;
import com.equo.comm.common.util.Pair;

/**
 * WebSocket server that relays messages to and from the event handler.
 */
public class EquoWebSocketServer extends WebSocketServer {
  private MessageHandler messageHandler = MessageHandler.getInstance();

  private HandlerContainer handlerContainer = HandlerContainer.getInstance();

  private boolean firstClientConnected = false;
  private List<String> messagesToSend = new ArrayList<>();

  private volatile boolean started = false;

  private static final EquoWebSocketServer INSTANCE = new EquoWebSocketServer();

  public static EquoWebSocketServer getInstance() {
    return INSTANCE;
  }

  /**
   * Starts websocket server when the service is activated.
   */
  private EquoWebSocketServer() {
    super(new InetSocketAddress(0));
    Logger.debug("Starting Equo websocket server...");
    start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        stop();
      } catch (Exception e) {
        // We tried...
      }
    }));
  }

  /**
   * Adds a Consumer event handler.
   * @param eventId       the action ID.
   * @param actionHandler the action handler.
   * @param paramTypes    types for the handler parameters.
   */
  public <T> void addEventHandler(String eventId, Consumer<T> actionHandler,
      Class<?>... paramTypes) {
    handlerContainer.putConsumer(eventId, actionHandler);
    if (paramTypes != null && paramTypes.length == 1) {
      handlerContainer.putActionParamType(eventId, paramTypes[0]);
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
    handlerContainer.putFunction(eventId, actionHandler);
    if (paramTypes != null && paramTypes.length == 1) {
      handlerContainer.putActionParamType(eventId, paramTypes[0]);
    }
  }

  /**
   * Adds a Function event handler.
   * @param eventId           the action ID.
   * @param responseTypeClass type of the expected response.
   */
  public <T> Future<T> addResponseHandler(String eventId, Class<T> responseTypeClass) {
    CompletableFuture<T> future = new CompletableFuture<T>();
    handlerContainer.putResponse(eventId,
        new Pair<CompletableFuture<?>, Class<?>>(future, responseTypeClass));
    return future;
  }

  /**
   * Removes an event handler.
   * @param actionId the action ID.
   */
  public void removeEventHandler(String actionId) {
    handlerContainer.removeEventHandler(actionId);
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    Logger.debug(
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
    Logger.debug(conn + " has left the Equo SDK!");
    broadcast(conn + " has left the Equo SDK!");
  }

  public void send(EventMessage eventMessage) {
    String messageAsJson = messageHandler.processMessageToSend(eventMessage);
    broadcast(messageAsJson);
  }

  /**
   * Called when a message arrives from javascript. Handles the message returning
   * a response if necessary.
   * @param message to receive from a handler.
   */
  public void receiveMessage(String message, boolean broadcast) {
    EventMessage eventMessage = messageHandler.parseEventMessage(message);
    if (eventMessage != null) {
      try {
        Optional<Object> response = messageHandler.processReceivedEventMessage(eventMessage);
        if (response.isPresent()) {
          EventMessage responseMessage =
              new EventMessage(eventMessage.getCallbackId(), response.get());
          String messageAsJson = messageHandler.processMessageToSend(responseMessage);
          super.broadcast(messageAsJson);
        }
      } catch (CommMessageException e) {
        final EventErrorMessage errorMessage = new EventErrorMessage(eventMessage.getCallbackId(),
            e.getErrorCode(), e.getLocalizedMessage());
        String errorMessageAsJson = messageHandler.processErrorMessageToSend(errorMessage);
        super.broadcast(errorMessageAsJson);
      }
    }
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    Logger.debug(conn + ": " + message);
    receiveMessage(message, true);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    Logger.debug(conn + ": " + message);
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
    Logger.debug("Equo Websocket Server started!");
  }

  @Override
  public void broadcast(String messageAsJson) {
    receiveMessage(messageAsJson, false);
    if (firstClientConnected) {
      super.broadcast(messageAsJson);
    } else {
      synchronized (messagesToSend) {
        messagesToSend.add(messageAsJson);
      }
    }
  }

  public boolean isStarted() {
    return started;
  }

}
