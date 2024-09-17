package com.joklek.rentbot.bot.callbacks;

import com.joklek.rentbot.bot.providers.ConfigInfoProvider;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(ConfigCallback.NAME)
public class ConfigCallback implements CallbackResponder {

    public static final String NAME = "config";
    private final UserRepo users;
    private final ConfigInfoProvider configInfoProvider;
    private final Map<String, CallbackAction> actions;

    public ConfigCallback(UserRepo users, ConfigInfoProvider configInfoProvider) {
        this.users = users;
        this.configInfoProvider = configInfoProvider;
        this.actions = Stream.of(
                new Toggle(users),
                new PriceMax(users),
                new PriceMin(users),
                new ConstructionMin(users),
                new FloorMin(users),
                new ToggleFees(users)
        ).collect(Collectors.toMap(CallbackAction::key, x -> x));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BaseRequest<?, ?> handle(Update update, TelegramBot bot, String... payload) {
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

    public InlineKeyboardMarkup showConfigPage(User user) {
        var keyboard = new InlineKeyboardMarkup();
        var enabledText = String.format("Notifications: %s", user.getEnabled() ? "Enabled" : "Disabled");
        var enabledButton = new InlineKeyboardButton(enabledText);
        enabledButton.callbackData(Toggle.CALLBACK_KEY);

        var priceMinText = String.format("Price from: %.2f EUR", user.getPriceMin().orElse(BigDecimal.ZERO));
        var priceMinButton = new InlineKeyboardButton(priceMinText);
        priceMinButton.callbackData(PriceMin.CALLBACK_KEY);

        var priceMaxText = String.format("Price to: %.2f EUR", user.getPriceMax().orElse(BigDecimal.ZERO));
        var priceMaxButton = new InlineKeyboardButton(priceMaxText);
        priceMaxButton.callbackData(PriceMax.CALLBACK_KEY);

        var roomsMinText = String.format("Min rooms: %d", user.getRoomsMin().orElse(0));
        var roomsMinButton = new InlineKeyboardButton(roomsMinText);
        roomsMinButton.callbackData(RoomsMin.CALLBACK_KEY);

        var roomsMaxText = String.format("Max rooms: %d", user.getRoomsMax().orElse(0));
        var roomsMaxButton = new InlineKeyboardButton(roomsMaxText);
        roomsMaxButton.callbackData(RoomsMax.CALLBACK_KEY);

        var constructionText = String.format("From construction year: %d", user.getYearMin().orElse(0));
        var constructionButton = new InlineKeyboardButton(constructionText);
        constructionButton.callbackData(ConstructionMin.CALLBACK_KEY);

        var floorText = String.format("Min floor: %d", user.getFloorMin().orElse(0));
        var floorButton = new InlineKeyboardButton(floorText);
        floorButton.callbackData(FloorMin.CALLBACK_KEY);

        var extraFeesText = String.format("Show with extra fees: %s", user.getShowWithFees() ? "Enabled" : "Disabled");
        var extraFeesButton = new InlineKeyboardButton(extraFeesText);
        extraFeesButton.callbackData(ToggleFees.CALLBACK_KEY);

        keyboard.addRow(enabledButton);
        keyboard.addRow(priceMinButton, priceMaxButton);
        keyboard.addRow(roomsMinButton, roomsMaxButton);
        keyboard.addRow(constructionButton);
        keyboard.addRow(floorButton);
        keyboard.addRow(extraFeesButton);

        return keyboard;
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
            updateMarkupRequest.replyMarkup(showConfigPage(user));

            var updateMessageRequest = new EditMessageText(message.chat().id(), message.messageId(), configInfoProvider.activeSettings(user));
            updateMessageRequest.parseMode(ParseMode.Markdown);

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

        private final UserRepo users;

        public PriceMin(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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

        private final UserRepo users;

        public PriceMax(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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

        private final UserRepo users;

        public RoomsMin(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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

        private final UserRepo users;

        public RoomsMax(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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

        private final UserRepo users;

        public ConstructionMin(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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

        private final UserRepo users;

        public FloorMin(UserRepo users) {
            this.users = users;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            var message = update.callbackQuery().maybeInaccessibleMessage();

            var updateMarkupRequest = new EditMessageReplyMarkup(message.chat().id(), message.messageId());
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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
            updateMarkupRequest.replyMarkup(showConfigPage(user));

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
