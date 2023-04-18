package com.equo.comm.common;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.equo.comm.api.error.CommMessageException;
import com.equo.comm.common.entity.EventErrorMessage;
import com.equo.comm.common.entity.EventMessage;
import com.equo.comm.common.util.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Service intended to be used by providers to handle common message processing.
 */
public class MessageHandler {

  private static final String MESSAGE_HANDLER_DOES_NOT_EXIST_ERROR =
      "An event handler does not exist for the user event ";

  private static Gson GSON_PARSER;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    JsonDeserializer<CommMessageException> deserializer =
        new JsonDeserializer<CommMessageException>() {
          @Override
          public CommMessageException deserialize(JsonElement json, Type typeOfT,
              JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            return new CommMessageException(jsonObject.get("code").getAsNumber().intValue(),
                jsonObject.get("message").getAsString());
          }
        };
    gsonBuilder.registerTypeAdapter(CommMessageException.class, deserializer);
    GSON_PARSER = gsonBuilder.create();
  }

  private HandlerContainer handlerContainer = HandlerContainer.getInstance();

  private static MessageHandler instance;

  /**
   * Returns the HandlerContainer singleton.
   * @return singleton instance
   */
  public static MessageHandler getInstance() {
    if (instance == null) {
      instance = new MessageHandler();
    }
    return instance;
  }

  private MessageHandler() {
  }

  /**
   * Parses the given message into an {@EventMessage}.
   * @param  message to parse
   * @return         eventMessage
   */
  public EventMessage parseEventMessage(String message) {
    EventMessage eventMessage = null;
    try {
      eventMessage = fromJson(message, EventMessage.class);
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
      jsonString = toJson(eventMessage.getPayload());
    }

    Pair<CompletableFuture<?>, Class<?>> pair = handlerContainer.getResponse(actionId);
    if (pair != null) {
      CompletableFuture<Object> future = (CompletableFuture<Object>) pair.getFirst();
      String messageError = eventMessage.getError();
      if (messageError != null) {
        CommMessageException messageException = fromJson(jsonString, CommMessageException.class);
        future.completeExceptionally(messageException);
      } else {
        Object parsedPayload = null;
        if (String.class.equals(pair.getSecond())) {
          parsedPayload = jsonString;
        } else {
          parsedPayload = fromJson(jsonString, pair.getSecond());
        }
        future.complete(parsedPayload);
      }
      return Optional.empty();
    }

    if (handlerContainer.getFunction(actionId) != null
        || handlerContainer.getConsumer(actionId) != null) {
      Object parsedPayload = null;
      if (eventMessage.getPayload() != null) {
        Class<?> type = handlerContainer.getActionParamType(actionId);
        if (String.class.equals(type)) {
          parsedPayload = jsonString;
        } else {
          parsedPayload = fromJson(jsonString, type);
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

    throw new CommMessageException(255,
        MESSAGE_HANDLER_DOES_NOT_EXIST_ERROR + "'" + actionId + "'");
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

  public String processErrorMessageToSend(EventErrorMessage eventMessage) {
    return toJson(eventMessage);
  }

  public <T> T fromJson(String json, Class<T> expectedType) {
    return GSON_PARSER.fromJson(json, expectedType);
  }

  public String toJson(Object obj) {
    return GSON_PARSER.toJson(obj);
  }

}
