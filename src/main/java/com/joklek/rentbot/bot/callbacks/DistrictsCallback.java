package com.joklek.rentbot.bot.callbacks;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.DistrictRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.response.BaseResponse;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(DistrictsCallback.NAME)
public class DistrictsCallback implements CallbackResponder {

    public static final String NAME = "districts";
    private final UserRepo users;
    private final DistrictRepo districts;
    private final Map<String, CallbackAction> actions;

    public DistrictsCallback(UserRepo users, DistrictRepo districts) {
        this.users = users;
        this.districts = districts;
        this.actions = Stream.of(
                new TurnOff(users),
                new TurnOn(users),
                new Reset(users),
                new Page(users)
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
        var telegramId = update.callbackQuery().message().chat().id();
        var user = users.getByTelegramId(telegramId);
        var payloadForAction = Arrays.copyOfRange(payload, 1, payload.length);
        action.action(user, update, bot, payloadForAction);

        return null;
    }

    private InlineKeyboardMarkup showTurnedOffPage() {
        var keyboard = new InlineKeyboardMarkup();
        var turnOnButton = new InlineKeyboardButton("✅ Turn on");
        turnOnButton.callbackData(TurnOn.CALLBACK_KEY);
        keyboard.addRow(turnOnButton);
        return keyboard;
    }

    private InlineKeyboardMarkup showPagedDistricts(User user, int page) {
        var keyboard = new InlineKeyboardMarkup();
        var allDistricts = districts.findAll();
        var userDistricts = districts.findByUsers(user);
        var pageSize = 6;
        var rowSize = 3;
        var pageCount = allDistricts.size() / pageSize;
        var nextPage = Math.min(page + 1, pageCount);
        var prevPage = Math.max(page - 1, 0);

        var fromIndex = page * pageSize;

        var firstRowDistricts = allDistricts.stream().skip(fromIndex).limit(rowSize).toList();
        var secondRowDistricts = allDistricts.stream().skip(fromIndex + rowSize).limit(rowSize).toList();

        var firstRowButtons = firstRowDistricts.stream().map(district -> {
            var name = userDistricts.contains(district) ? String.format("✅%s", district.getName()) : district.getName();
            var districtButton = new InlineKeyboardButton(name);
            districtButton.callbackData(String.format("/f:districts:%d", district.getId()));
            return districtButton;
        }).toList();
        var secondRowButtons = secondRowDistricts.stream().map(district -> {
            var name = userDistricts.contains(district) ? String.format("✅%s", district.getName()) : district.getName();
            var districtButton = new InlineKeyboardButton(name);
            districtButton.callbackData(String.format("/f:districts:%d", district.getId()));
            return districtButton;
        }).toList();

        var prevButton = new InlineKeyboardButton("⬅");
        prevButton.callbackData(DistrictsCallback.Page.callbackKey(prevPage));
        var nextButton = new InlineKeyboardButton("➡");
        nextButton.callbackData(DistrictsCallback.Page.callbackKey(nextPage));
        var resetButton = new InlineKeyboardButton("\uD83D\uDD04");
        resetButton.callbackData(Reset.CALLBACK_KEY);
        var turnOffButton = new InlineKeyboardButton("❌");
        turnOffButton.callbackData(TurnOff.CALLBACK_KEY);
        keyboard.addRow(firstRowButtons.toArray(InlineKeyboardButton[]::new));
        keyboard.addRow(secondRowButtons.toArray(InlineKeyboardButton[]::new));
        keyboard.addRow(prevButton, nextButton, resetButton, turnOffButton); // control row
        return keyboard;
    }

    public interface CallbackAction {
        void action(User user, Update update, TelegramBot bot, String... payload);

        String callbackKey();

        String key();
    }

    public class TurnOn implements CallbackAction {
        public static final String KEY = "on";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public TurnOn(UserRepo users) {
            this.users = users;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            user.setFilterByDistrict(true);
            users.save(user);

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId());
            updateMarkupRequest.replyMarkup(showPagedDistricts(user, 0));

            var updateMessageRequest = new EditMessageText(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId(), "TODO");
            bot.execute(updateMessageRequest, new Callback<EditMessageText, BaseResponse>() {
                @Override
                public void onResponse(EditMessageText request, BaseResponse response) {
                    bot.execute(updateMarkupRequest);
                }

                @Override
                public void onFailure(EditMessageText request, IOException e) {

                }
            });
        }
    }

    public class TurnOff implements CallbackAction {
        public static final String KEY = "off";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public TurnOff(UserRepo users) {
            this.users = users;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            user.setFilterByDistrict(false);
            users.save(user);

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId());
            updateMarkupRequest.replyMarkup(showTurnedOffPage());

            var updateMessageRequest = new EditMessageText(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId(), "TODO");
            bot.execute(updateMessageRequest, new Callback<EditMessageText, BaseResponse>() {
                @Override
                public void onResponse(EditMessageText request, BaseResponse response) {
                    bot.execute(updateMarkupRequest);
                }

                @Override
                public void onFailure(EditMessageText request, IOException e) {
                }
            });
        }
    }

    public class Reset implements CallbackAction {
        public static final String KEY = "reset";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public Reset(UserRepo users) {
            this.users = users;
        }


        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        @Override
        @Transactional
        public void action(User user, Update update, TelegramBot bot, String... payload) {
//            user.getDistricts().clear(); // TODO
//            users.save(user);

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId());
            updateMarkupRequest.replyMarkup(showPagedDistricts(user, 0));
            bot.execute(updateMarkupRequest);

            var updateMessageRequest = new EditMessageText(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId(), "TODO");
            bot.execute(updateMessageRequest, new Callback<EditMessageText, BaseResponse>() {
                @Override
                public void onResponse(EditMessageText request, BaseResponse response) {
                    var callbackResponse = new AnswerCallbackQuery(update.callbackQuery().id());
                    callbackResponse.text("List cleared");
                    bot.execute(callbackResponse);
                }

                @Override
                public void onFailure(EditMessageText request, IOException e) {
                }
            });
        }
    }

    public class Page implements CallbackAction {
        public static final String KEY = "page";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;

        public Page(UserRepo users) {
            this.users = users;
        }


        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        public static String callbackKey(int page) {
            return String.format("%s:%s", CALLBACK_KEY, page);
        }

        @Override
        @Transactional
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            if (payload.length == 0) {
                return;
            }
            var pageRaw = payload[0];
            var page = Integer.parseInt(pageRaw); // TODO validate pls

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().message().chat().id(), update.callbackQuery().message().messageId());
            updateMarkupRequest.replyMarkup(showPagedDistricts(user, page));
            bot.execute(updateMarkupRequest);
        }
    }
}
