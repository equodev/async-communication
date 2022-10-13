package com.equo.comm.common.service;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.comm.api.ICommSendService;
import com.equo.comm.common.handler.ISendEventHandler;

/**
 * Implements the handler actions for send events.
 */
@Component
public class CommSendService implements ICommSendService {

  @Reference
  private ISendEventHandler sendEventHandler;

  public void send(String userEvent) {
    sendEventHandler.send(userEvent, null);
  }

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

}
