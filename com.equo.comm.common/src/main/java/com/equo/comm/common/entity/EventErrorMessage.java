package com.equo.comm.common.entity;

/**
 * Model to use for error message serialization.
 */
public class EventErrorMessage {

  private String actionId;
  private Error error;

  public EventErrorMessage() {
  }

  /**
   * Parameterized constructor.
   */
  public EventErrorMessage(String actionId, int errorCode, String errorMessage) {
    this.actionId = actionId;
    this.error = new Error(errorCode, errorMessage);
  }

  public String getActionId() {
    return actionId;
  }

  public void setActionId(String actionId) {
    this.actionId = actionId;
  }

  public Error getError() {
    return error;
  }

  public void setError(Error error) {
    this.error = error;
  }

  @SuppressWarnings("unused")
  private static class Error {
    private int code;
    private String message;

    public Error(int code, String message) {
      this.code = code;
      this.message = message;
    }
  }

}
