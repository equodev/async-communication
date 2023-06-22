package com.equo.comm.api;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

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
  public <T> Future<T> send(String userEvent, Class<T> responseTypeClass);

  /**
   * Sends the specified data to later be transmitted using the userEvent as ID
   * expecting a response.
   * @param userEvent the event ID.
   * @param payload   the data to send.
   */
  public <T> Future<T> send(String userEvent, Object payload, Class<T> responseTypeClass);

  /**
   * Tries to find a reference to the service via all supported mechanisms.
   * @return A reference to the service if found, null otherwise.
   */
  public static ICommSendService findServiceReference() {
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

    ServiceLoader<ICommSendService> serviceLoader = ServiceLoader.load(ICommSendService.class);
    Iterator<ICommSendService> it = serviceLoader.iterator();
    if (it.hasNext()) {
      return it.next();
    }
    return null;
  }

}
