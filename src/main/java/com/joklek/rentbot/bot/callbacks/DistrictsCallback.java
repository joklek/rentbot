package com.joklek.rentbot.bot.callbacks;

import com.joklek.rentbot.bot.providers.DistrictsPageProvider;
import com.joklek.rentbot.entities.District;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.DistrictRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(DistrictsCallback.NAME)
public class DistrictsCallback implements CallbackResponder {

    public static final String NAME = "districts";
    private final UserRepo users;
    private final DistrictsPageProvider districtsPageProvider;
    private final Map<String, CallbackAction> actions;

    public DistrictsCallback(UserRepo users, DistrictRepo districts, DistrictsPageProvider districtsPageProvider) {
        this.users = users;
        this.districtsPageProvider = districtsPageProvider;
        this.actions = Stream.of(
                new TurnOff(users),
                new TurnOn(users),
                new Reset(users),
                new Page(),
                new Toggle(users, districts)
        ).collect(Collectors.toMap(CallbackAction::key, x -> x));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @Transactional
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

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
            updateMarkupRequest.replyMarkup(districtsPageProvider.showPagedDistricts(user, 0));

            var updateMessageRequest = new EditMessageText(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId(), "Please select your wanted districts. If none are selected all listings will be shown. Listings without any district will always be shown. Please note that some sites have different district classifications or names.");
            bot.execute(updateMessageRequest);
            bot.execute(updateMarkupRequest);
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

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
            updateMarkupRequest.replyMarkup(districtsPageProvider.showTurnedOffPage());

            var updateMessageRequest = new EditMessageText(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId(), "There is a possibility to filter listings by district. Listings without any district will always be shown. Please note that some sites have different district classifications or names.");
            bot.execute(updateMessageRequest);
            bot.execute(updateMarkupRequest);
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
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            user.getDistricts().clear(); // TODO
            users.save(user);

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
            updateMarkupRequest.replyMarkup(districtsPageProvider.showPagedDistricts(user, 0));

            var updateMessageRequest = new EditMessageText(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId(), "Please select your wanted districts. If none are selected all listings will be shown. Listings without any district will always be shown. Please note that some sites have different district classifications or names.");
            var callbackResponse = new AnswerCallbackQuery(update.callbackQuery().id());
            callbackResponse.text("List cleared");
            bot.execute(updateMessageRequest);
            bot.execute(updateMarkupRequest);
            bot.execute(callbackResponse);
        }
    }

    public class Page implements CallbackAction {
        public static final String KEY = "page";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

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
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            if (payload.length == 0) {
                return;
            }
            var pageRaw = payload[0];
            var page = Integer.parseInt(pageRaw); // TODO validate pls

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
            updateMarkupRequest.replyMarkup(districtsPageProvider.showPagedDistricts(user, page));
            bot.execute(updateMarkupRequest);
        }
    }

    public class Toggle implements CallbackAction {
        public static final String KEY = "toggle";
        public static final String CALLBACK_KEY = String.format("/f%s:%s", NAME, KEY);

        private final UserRepo users;
        private final DistrictRepo districts;

        public Toggle(UserRepo users, DistrictRepo districts) {
            this.users = users;
            this.districts = districts;
        }


        @Override
        public String key() {
            return KEY;
        }

        @Override
        public String callbackKey() {
            return CALLBACK_KEY;
        }

        public static String callbackKey(long districtId) {
            return String.format("%s:%s", CALLBACK_KEY, districtId);
        }

        @Override
        public void action(User user, Update update, TelegramBot bot, String... payload) {
            if (payload.length == 0) {
                return;
            }
            var pageRaw = payload[0];
            var districtId = Long.parseLong(pageRaw); // TODO validate pls

            var district = districts.getReferenceById(districtId); // TODO catch if not exists

            if (user.getDistricts().contains(district)) {
                user.getDistricts().remove(district);
            } else {
                user.getDistricts().add(district);
            }
            users.save(user);

            var page = districts.findAllByOrderByNameAsc().stream().map(District::getName).toList().indexOf(district.getName()) / DistrictsPageProvider.PAGE_SIZE;

            var updateMarkupRequest = new EditMessageReplyMarkup(update.callbackQuery().maybeInaccessibleMessage().chat().id(), update.callbackQuery().maybeInaccessibleMessage().messageId());
            updateMarkupRequest.replyMarkup(districtsPageProvider.showPagedDistricts(user, page)); // TODO how to refresh properly?
            bot.execute(updateMarkupRequest);
        }
    }
}
