package com.equo.comm.ws.provider;

/**
 * This class provides methods for logging debug messages.
 */
public class Logger {
  private static final boolean debug = Boolean.getBoolean("chromium.debug");

  /**
   * Prints a debug message to the console if debug mode is enabled.
   * @param string The debug message to print.
   */
  public static void debug(String string) {
    if (debug) {
      System.out.println("[debug]: " + string);
    }
  }
}
