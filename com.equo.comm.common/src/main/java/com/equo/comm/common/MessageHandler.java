package com.equo.comm.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.equo.comm.api.actions.IActionHandler;
import com.equo.comm.api.annotations.EventName;
import com.equo.comm.api.error.CommMessageException;
import com.equo.comm.common.entity.EventErrorMessage;
import com.equo.comm.common.entity.EventMessage;
import com.equo.comm.common.util.ActionHelper;
import com.equo.comm.common.util.Pair;
import com.google.gson.Gson;

/**
 * Service intended to be used by providers to handle common message processing.
 */
@Component(service = MessageHandler.class)
public class MessageHandler {

  private static Gson GSON_PARSER = new Gson();

  @Reference
  private HandlerContainer handlerContainer;

  /**
   * Parses the given message into an {@EventMessage}.
   * @param  message to parse
   * @return         eventMessage
   */
  public EventMessage parseEventMessage(String message) {
    EventMessage eventMessage = null;
    try {
      eventMessage = GSON_PARSER.fromJson(message, EventMessage.class);
    } catch (Exception e) {
      // TODO: throw IllegalArgumentException
    }
    return eventMessage;
  }

  /**
   * Takes an event message and processes it accordingly. Returns the wrapped
   * response or an empty Optional.
   * @param  eventMessage to process
   * @return              the handler response
   */
  @SuppressWarnings("unchecked")
  public Optional<Object> processReceivedEventMessage(EventMessage eventMessage) {
    String actionId = eventMessage.getActionId();

    String jsonString;
    if (eventMessage.getPayload() instanceof String) {
      jsonString = eventMessage.getPayload().toString();
    } else {
      jsonString = GSON_PARSER.toJson(eventMessage.getPayload());
    }

    Pair<CompletableFuture<?>, Class<?>> pair = handlerContainer.getResponse(actionId);
    if (pair != null) {
      CompletableFuture<Object> future = (CompletableFuture<Object>) pair.getFirst();
      String messageError = eventMessage.getError();
      if (messageError != null) {
        future.completeExceptionally(new CommMessageException(-1, messageError));
      } else {
        Object parsedPayload = null;
        if (String.class.equals(pair.getSecond())) {
          parsedPayload = jsonString;
        } else {
          parsedPayload = GSON_PARSER.fromJson(jsonString, pair.getSecond());
        }
        future.complete(parsedPayload);
      }
    }

    if (handlerContainer.getFunction(actionId) != null
        || handlerContainer.getConsumer(actionId) != null) {
      Object parsedPayload = null;
      if (eventMessage.getPayload() != null) {
        Class<?> type = handlerContainer.getActionParamType(actionId);
        if (String.class.equals(type)) {
          parsedPayload = jsonString;
        } else {
          parsedPayload = GSON_PARSER.fromJson(jsonString, type);
        }
      }
      Function<?, ?> function = handlerContainer.getFunction(actionId);
      Object response = null;
      if (function != null) {
        response = ((Function<Object, ?>) function).apply(parsedPayload);
      } else {
        Consumer<?> consumer = handlerContainer.getConsumer(actionId);
        ((Consumer<Object>) consumer).accept(parsedPayload);
      }
      if (response != null) {
        if (response instanceof String) {
          return Optional.of((String) response);
        }
        return Optional.of(response);
      } else {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  /**
   * Takes an event message and converts it to JSON format, handling special cases
   * if applies.
   * @param  eventMessage to process
   * @return              JSON processed from the eventMessage
   */
  public String processMessageToSend(EventMessage eventMessage) {
    Object payload = eventMessage.getPayload();
    if (payload != null) {
      if (payload instanceof Byte[] || payload instanceof byte[]) {
        eventMessage.setPayload(Base64.getEncoder().encodeToString((byte[]) payload));
      }
    }
    return toJson(eventMessage);
  }

  public String toJson(Object obj) {
    return GSON_PARSER.toJson(obj);
  }

  public String processErrorMessageToSend(EventErrorMessage eventMessage) {
    return GSON_PARSER.toJson(eventMessage);
  }

  /**
   * Method used to add all the Action Handler implementations.
   */
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
      policyOption = ReferencePolicyOption.GREEDY)
  public void setFunctionActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      final Class<?> parameterType;
      Type[] types = method.getGenericParameterTypes();
      if (types.length == 1) {
        parameterType = (Class<?>) types[0];
        handlerContainer.putActionParamType(actionHandlerName, parameterType);
      } else {
        parameterType = Object.class;
      }
      Class<?> rt = method.getReturnType();
      if (Void.TYPE.equals(rt)) {
        Consumer<?> cons = (param) -> {
          try {
            method.invoke(actionHandler, parameterType.cast(param));
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
              | ClassCastException e1) {
            try {
              method.invoke(actionHandler);
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e2) {
              return;
            }
          }
        };
        handlerContainer.putConsumer(actionHandlerName, cons);
      } else {
        Function<?, ?> func = (param) -> {
          try {
            return method.invoke(actionHandler, parameterType.cast(param));
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
              | ClassCastException e1) {
            try {
              return method.invoke(actionHandler);
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e2) {
              return null;
            }
          }
        };
        handlerContainer.putFunction(actionHandlerName, func);
      }
    }
  }

  /**
   * Method to release all actions defined in this action handler.
   */
  public void unsetFunctionActionHandler(IActionHandler actionHandler) {
    for (Method method : actionHandler.getClass().getDeclaredMethods()) {
      final String actionHandlerName =
          ActionHelper.getEventName(method.getAnnotation(EventName.class)).orElse(method.getName());
      handlerContainer.removeFunction(actionHandlerName);
      handlerContainer.removeConsumer(actionHandlerName);
      handlerContainer.removeActionParamType(actionHandlerName);
    }
  }

}
