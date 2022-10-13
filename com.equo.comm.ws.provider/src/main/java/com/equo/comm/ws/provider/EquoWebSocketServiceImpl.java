/****************************************************************************
**
** Copyright (C) 2021 Equo
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

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.comm.common.entity.EventMessage;
import com.equo.comm.common.handler.IReceiveEventHandler;
import com.equo.comm.common.handler.ISendEventHandler;
import com.equo.logging.client.api.Logger;
import com.equo.logging.client.api.LoggerFactory;

/**
 * Websocket service implementation. Manages the websocket server lifecycle and
 * all the event listeners.
 */
@Component
public class EquoWebSocketServiceImpl implements IReceiveEventHandler, ISendEventHandler {

  protected static final Logger LOGGER = LoggerFactory.getLogger(EquoWebSocketServiceImpl.class);

  @Reference
  private EquoWebSocketServer equoWebSocketServer;

  @Override
  public <T> void addEventHandler(String eventId, Consumer<T> actionHandler,
      Class<?>... paramTypes) {
    equoWebSocketServer.addEventHandler(eventId, actionHandler, paramTypes);
  }

  @Override
  public <T, R> void addEventHandler(String eventId, Function<T, R> actionHandler,
      Class<?>... paramTypes) {
    equoWebSocketServer.addEventHandler(eventId, actionHandler, paramTypes);
  }

  @Override
  public void send(String userEvent, Object payload) {
    EventMessage eventMessage = new EventMessage(userEvent, payload);
    equoWebSocketServer.send(eventMessage);
  }

  @Override
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass) {
    String uuid = UUID.randomUUID().toString();
    EventMessage eventMessage = new EventMessage(userEvent, payload, uuid);
    Future<T> future = equoWebSocketServer.addResponseHandler(uuid, responseTypeClass);
    equoWebSocketServer.send(eventMessage);
    return future;
  }

  /**
   * Gets the port number that this server listens on.
   * @return the port number.
   */
  public int getPort() {
    while (!equoWebSocketServer.isStarted()) {
    }
    return equoWebSocketServer.getPort();
  }

  @Override
  public void removeEventHandler(String actionId) {
    equoWebSocketServer.removeEventHandler(actionId);
  }
}
