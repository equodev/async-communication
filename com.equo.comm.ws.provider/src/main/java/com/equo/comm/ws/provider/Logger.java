package com.equo.comm.ws.provider;

public class Logger {
	private static final boolean debug = Boolean.getBoolean("chromium.debug");

    public static void debug(String string) {
        if (debug) {
            System.out.println("[debug]: " + string);
        }
    }
}