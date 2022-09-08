package com.equo.comm.api.internal;

import java.util.concurrent.Future;

/**
 * Interface of the Comm send service.
 */
public interface ISendEventHandler {

  /**
   * Sends the specified data to later be transmitted using the userEvent as ID.
   * @param userEvent the event ID.
   * @param payload   the data to send.
   */
  public void send(String userEvent, Object payload);

  /**
   * Sends the specified data to later be transmitted using the userEvent as ID.
   * @param userEvent         the event ID.
   * @param payload           the data to send.
   * @param responseTypeClass the class with which the response will be received
   */
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass);

}
