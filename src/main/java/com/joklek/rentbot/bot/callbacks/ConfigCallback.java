package com.joklek.rentbot.bot.callbacks;

import com.joklek.rentbot.bot.providers.ConfigInfoProvider;
import com.joklek.rentbot.bot.replies.*;
import com.joklek.rentbot.entities.SentMessage;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.SentMessageRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(ConfigCallback.NAME)
public class ConfigCallback implements CallbackResponder {

    public static final String NAME = "config";
    private final UserRepo users;
    private final ConfigInfoProvider configInfoProvider;
    private final SentMessageRepo sentMessages;
    private final Map<String, CallbackAction> actions;

    public ConfigCallback(UserRepo users, ConfigInfoProvider configInfoProvider, SentMessageRepo sentMessages) {
        this.users = users;
        this.configInfoProvider = configInfoProvider;
        this.sentMessages = sentMessages;
        this.actions = Stream.of(
                new Toggle(users),
                new PriceMin(),
                new PriceMax(),
                new RoomsMin(),
                new RoomsMax(),
                new ConstructionMin(),
                new FloorMin(),
                new ToggleFees(users)
        ).collect(Collectors.toMap(CallbackAction::key, x -> x));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BaseRequest<?, ?> handle(Update update, TelegramBot bot, String... payload) {
        var maybeConfigMessage = sentMessages.findByChatIdAndMessageId(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
        if (maybeConfigMessage.isEmpty()) {
            var configMessage = new SentMessage(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId(), NAME);
            sentMessages.save(configMessage);
        }

        if (payload.length == 0) {
            return null;
        }
        var actionKey = payload[0];
        var action = actions.get(actionKey);
        if (action == null) {
            return null;
        }
        var telegramId = update.callbackQuery().maybeInaccessibleMessage().chat().id();
        var user = users.getByTelegramId(telegramId);
        var payloadForAction = Arrays.copyOfRange(payload, 1, payload.length);
        action.action(user, update, bot, payloadForAction);

        return null;
    }

    public interface CallbackAction {
        void action(User user, Update update, TelegramBot bot, String... payload);

        String callbackKey();

        String key();
    }

    public class Toggle implements CallbackAction {
        public static final String KEY = "toggle";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public Toggle(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();
            var settingsChanged = false;
            if (user.getEnabled() || user.isConfigured()) {
                user.setEnabled(!user.getEnabled());
                users.save(user);
                settingsChanged = true;
            }

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            if (!settingsChanged) {
                var callbackResponse = new AnswerCallbackQuery(update.callbackQuery().id());
                callbackResponse.text("Configure other settings first");
                callbackResponse.showAlert(true);
                bot.execute(callbackResponse);
            } else {
                bot.execute(updateMarkupRequest);
            }
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class PriceMin implements CallbackAction {
        public static final String KEY = "priceMin";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new min price in EUR?");
            var reply = new ForceReply(false, "Enter new min price in EUR");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("PriceMin question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class PriceMax implements CallbackAction {
        public static final String KEY = "priceMax";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new max price in EUR?");
            var reply = new ForceReply(false, "Enter new max price in EUR");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("PriceMax question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class RoomsMin implements CallbackAction {
        public static final String KEY = "roomsMin";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new min rooms?");
            var reply = new ForceReply(false, "Enter new min rooms");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("RoomsMin question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class RoomsMax implements CallbackAction {
        public static final String KEY = "roomsMax";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new max rooms?");
            var reply = new ForceReply(false, "Enter new max rooms");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("RoomsMax question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class ConstructionMin implements CallbackAction {
        public static final String KEY = "constructionMin";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new min contruction year?");
            var reply = new ForceReply(false, "Enter new min construction year");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("ConstructionMin question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class FloorMin implements CallbackAction {
        public static final String KEY = "floorMin";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var sendMessage = new SendMessage(message.chat().id(), "What is the new min floor?");
            var reply = new ForceReply(false, "Enter new min floor");
            sendMessage.replyMarkup(reply);
            bot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage request, SendResponse response) {
                    System.out.printf("FloorMin question in Thread %d message: %s%n", response.message().chat().id(), response.message().messageId());
                }

                @Override
                public void onFailure(SendMessage request, IOException e) {
                }
            });
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }

    public class ToggleFees implements CallbackAction {
        public static final String KEY = "toggleFee";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public ToggleFees(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();
            user.setShowWithFees(!user.getShowWithFees());
            users.save(user);

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(configInfoProvider.showConfigPage(user));

            var updateMessageRequest = new EditMessageText(message.chat().id(), message.messageId(), configInfoProvider.activeSettings(user));
            updateMessageRequest.parseMode(ParseMode.Markdown);

            bot.execute(updateMessageRequest);
            bot.execute(updateMarkupRequest);
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public String key() {
            return KEY;
        }
    }
}
