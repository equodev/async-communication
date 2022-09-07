package com.equo.comm.api;

import java.util.concurrent.Future;

/**
 * Allows to send events.
 */
public interface ICommSendService {

  /**
   * Sends a null data to later be transmitted using the userEvent as ID.
   * @param userEvent the event ID.
   */
  public void send(String userEvent);

  /**
   * Sends the specified data to later be transmitted using the userEvent as ID.
   * @param userEvent the event ID.
   * @param payload   the data to send.
   */
  public void send(String userEvent, Object payload);

  /**
   * Sends a null data to later be transmitted using the userEvent as ID expecting
   * a response.
   * @param userEvent the event ID.
   */
  <T> Future<T> send(String userEvent, Class<T> responseTypeClass);

  /**
   * Sends the specified data to later be transmitted using the userEvent as ID
   * expecting a response.
   * @param userEvent the event ID.
   * @param payload   the data to send.
   */
  <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass);

}
