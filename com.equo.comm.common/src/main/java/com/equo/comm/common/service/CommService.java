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

package com.equo.comm.common.service;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.comm.api.ICommService;
import com.equo.comm.common.handler.IReceiveEventHandler;
import com.equo.comm.common.handler.ISendEventHandler;
import com.equo.comm.common.handler.NoOpSendEventHandler;

/**
 * Implements the handler actions for send and receive events.
 */
@Component(immediate = true)
public class CommService implements ICommService {

  private static final NoOpSendEventHandler NO_OP_SEND_HANDLER = new NoOpSendEventHandler();

  @Reference
  private IReceiveEventHandler receiveEventHandler;

  private volatile ISendEventHandler sendEventHandler = NO_OP_SEND_HANDLER;

  @Override
  public void send(String userEvent) {
    sendEventHandler.send(userEvent, null);
  }

  @Override
  public void send(String userEvent, Object payload) {
    sendEventHandler.send(userEvent, payload);
  }

  @Override
  public <T> Future<T> send(String userEvent, Class<T> responseTypeClass) {
    return sendEventHandler.send(userEvent, null, responseTypeClass);
  }

  @Override
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass) {
    return sendEventHandler.send(userEvent, payload, responseTypeClass);
  }

  @Override
  public <T, R> void on(String actionId, Class<T> payloadClass, Function<T, R> actionHandler) {
    receiveEventHandler.addEventHandler(actionId, actionHandler, payloadClass);
  }

  @Override
  public <R> void on(String actionId, Function<String, R> actionHandler) {
    receiveEventHandler.addEventHandler(actionId, actionHandler, String.class);
  }

  @Override
  public <T> void on(String actionId, Class<T> payloadClass, Consumer<T> actionHandler) {
    receiveEventHandler.addEventHandler(actionId, actionHandler, payloadClass);
  }

  @Override
  public void on(String actionId, Consumer<String> actionHandler) {
    receiveEventHandler.addEventHandler(actionId, actionHandler, String.class);
  }

  @Override
  public void remove(String actionId) {
    receiveEventHandler.removeEventHandler(actionId);
  }

  @Reference(cardinality = OPTIONAL, policy = DYNAMIC)
  public void bindSendEventHandler(ISendEventHandler sendEventHandler) {
    this.sendEventHandler = sendEventHandler;
    NO_OP_SEND_HANDLER.flush(sendEventHandler);
  }

  public void unbindSendEventHandler(ISendEventHandler sendEventHandler) {
    this.sendEventHandler = NO_OP_SEND_HANDLER;
  }

}
