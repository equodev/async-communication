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

package com.equo.comm.api.internal;

/**
 * An action message with parameters.
 */
public class EventMessage {

  private String actionId;
  private String callbackId;
  private Object payload;
  private String error;

  public EventMessage() {
  }

  public EventMessage(String actionId, Object payload) {
    this.actionId = actionId;
    this.payload = payload;
  }

  /**
   * Simple constructor. Initializes this instance's attributes with the given
   * values.
   * @param actionId   ID of this action
   * @param payload    parameters of this action
   * @param callbackId ID of this action's callback
   */
  public EventMessage(String actionId, Object payload, String callbackId) {
    this.actionId = actionId;
    this.payload = payload;
    this.callbackId = callbackId;
  }

  public String getActionId() {
    return actionId;
  }

  public Object getPayload() {
    return payload;
  }

  public String getCallbackId() {
    return callbackId;
  }

  public String getError() {
    return error;
  }

  public void setActionId(String actionId) {
    this.actionId = actionId;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

  public void setCallbackId(String callbackId) {
    this.callbackId = callbackId;
  }

  public void setError(String error) {
    this.error = error;
  }

}
