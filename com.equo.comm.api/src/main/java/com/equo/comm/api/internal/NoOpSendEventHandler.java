package com.equo.comm.api.internal;

import java.util.concurrent.Future;

/**
 * NoOp implementation for send events.
 */
public class NoOpSendEventHandler implements ISendEventHandler {

  @Override
  public void send(String userEvent, Object payload) {
  }

  @Override
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass) {
    return null;
  }

}
