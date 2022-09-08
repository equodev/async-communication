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

package com.equo.comm.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for event handlers. Allows to listen and send events.
 */
public interface ICommService extends ICommSendService {

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param payloadClass  the event expected payload class.
   * @param actionHandler the action handler.
   */
  <T, R> void on(String actionId, Class<T> payloadClass, Function<T, R> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param actionId      the event id.
   * @param actionHandler the action handler.
   */
  <R> void on(String actionId, Function<String, R> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param payloadClass  the event expected payload class.
   * @param actionHandler the action handler.
   */
  <T> void on(String actionId, Class<T> payloadClass, Consumer<T> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param actionId      the event id.
   * @param actionHandler the action handler.
   */
  void on(String actionId, Consumer<String> actionHandler);

  /**
   * Removes an event handler.
   * @param actionId the action ID associated with the handler to remove.
   */
  void remove(String actionId);

}
