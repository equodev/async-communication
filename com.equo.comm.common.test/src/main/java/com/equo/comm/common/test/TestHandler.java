package com.equo.comm.common.test;

import org.osgi.service.component.annotations.Component;

import com.equo.comm.api.actions.IActionHandler;

@Component
public class TestHandler implements IActionHandler {

  public String testHandler(String payload) {
    return payload + "1";
  }

}
