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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Interface for event handlers. Allows to listen and send events.
 */
public interface ICommService extends ICommSendService {

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param payloadClass  the event expected payload class.
   * @param actionHandler the action handler.
   */
  public <T, R> void on(String actionId, Class<T> payloadClass, Function<T, R> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param actionId      the event id.
   * @param actionHandler the action handler.
   */
  public <R> void on(String actionId, Function<String, R> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param payloadClass  the event expected payload class.
   * @param actionHandler the action handler.
   */
  public <T> void on(String actionId, Class<T> payloadClass, Consumer<T> actionHandler);

  /**
   * Defines a custom actionHandler for an specific event ID.
   * @param actionId      the event id.
   * @param actionHandler the action handler.
   */
  public void on(String actionId, Consumer<String> actionHandler);

  /**
   * Removes an event handler.
   * @param actionId the action ID associated with the handler to remove.
   */
  void remove(String actionId);

  /**
   * Tries to find a reference to the service via all supported mechanisms.
   * @return A reference to the service if found, null otherwise.
   */
  public static ICommService findServiceReference() {
    try {
      BundleContext ctx = FrameworkUtil.getBundle(ICommService.class).getBundleContext();
      if (ctx != null) {
        ServiceReference<ICommService> serviceReference =
            ctx.getServiceReference(ICommService.class);
        if (serviceReference != null) {
          return ctx.getService(serviceReference);
        }
      }
    } catch (Exception e) {
      // Non-OSGi environments
    }

    ServiceLoader<ICommService> serviceLoader = ServiceLoader.load(ICommService.class);
    Iterator<ICommService> it = serviceLoader.iterator();
    if (it.hasNext()) {
      return it.next();
    }
    return null;
  }

}
