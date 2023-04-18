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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.equo.comm.api.ICommSendService;
import com.equo.comm.api.ICommService;
import com.equo.comm.api.actions.IActionHandler;
import com.equo.comm.common.HandlerContainer;
import com.equo.comm.common.entity.EventMessage;
import com.equo.logging.client.api.Logger;
import com.equo.logging.client.api.LoggerFactory;

/**
 * Websocket service implementation. Manages the websocket server lifecycle and
 * all the event listeners.
 */
@Component
public class EquoWebSocketServiceImpl implements ICommService, ICommSendService {

  protected static final Logger LOGGER = LoggerFactory.getLogger(EquoWebSocketServiceImpl.class);

  private EquoWebSocketServer equoWebSocketServer = EquoWebSocketServer.getInstance();

  private <T> void addEventHandler(String eventId, Consumer<T> actionHandler,
      Class<?>... paramTypes) {
    equoWebSocketServer.addEventHandler(eventId, actionHandler, paramTypes);
  }

  private <T, R> void addEventHandler(String eventId, Function<T, R> actionHandler,
      Class<?>... paramTypes) {
    equoWebSocketServer.addEventHandler(eventId, actionHandler, paramTypes);
  }

  @Override
  public void send(String userEvent) {
    send(userEvent, (Object) null);
  }

  @Override
  public void send(String userEvent, Object payload) {
    EventMessage eventMessage = new EventMessage(userEvent, payload);
    equoWebSocketServer.send(eventMessage);
  }

  @Override
  public <T> Future<T> send(String userEvent, Class<T> responseTypeClass) {
    return send(userEvent, null, responseTypeClass);
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

  public void removeEventHandler(String actionId) {
    equoWebSocketServer.removeEventHandler(actionId);
  }

  @Override
  public <T, R> void on(String actionId, Class<T> payloadClass, Function<T, R> actionHandler) {
    addEventHandler(actionId, actionHandler, payloadClass);
  }

  @Override
  public <R> void on(String actionId, Function<String, R> actionHandler) {
    addEventHandler(actionId, actionHandler, String.class);
  }

  @Override
  public <T> void on(String actionId, Class<T> payloadClass, Consumer<T> actionHandler) {
    addEventHandler(actionId, actionHandler, payloadClass);
  }

  @Override
  public void on(String actionId, Consumer<String> actionHandler) {
    addEventHandler(actionId, actionHandler, String.class);
  }

  public void remove(String actionId) {
    equoWebSocketServer.removeEventHandler(actionId);
  }

  private HandlerContainer handlerContainer = HandlerContainer.getInstance();

  /**
   * Method used to add all the Action Handler implementations.
   */
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void addActionHandler(IActionHandler actionHandler) {
    handlerContainer.addActionHandler(actionHandler);
  }

  /**
   * Method to release all actions defined in this action handler.
   */
  public void removeActionHandler(IActionHandler actionHandler) {
    handlerContainer.removeActionHandler(actionHandler);
  }

}
