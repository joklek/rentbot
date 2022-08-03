package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.validation.ValidationException;
import javax.validation.Validator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ConfigCommand implements Command {

    private static final Logger LOGGER = getLogger(ConfigCommand.class);

    private static final Pattern CONFIG_PATTERN = Pattern.compile("^(\\d{1,5}) (\\d{1,5}) (\\d{1,2}) (\\d{1,2}) (\\d{4}) (\\d{1,3}) (yes|no)$");
    private static final String CONFIG_TEXT = "Use this format:\n\n```\n/config <price_from> <price_to> <rooms_from> <rooms_to> <year_from> <min_flor> <show with fee?(yes/no)>\n```\nExample:\n```\n/config 200 330 1 2 2000 2 yes\n```";

    private final UserRepo users;
    private final Validator validator;

    public ConfigCommand(UserRepo users, Validator validator) {
        this.users = users;
        this.validator = validator;
    }

    @Override
    public String command() {
        return "/config";
    }

    @Override
    public BaseRequest<?, ?> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if (payload.isBlank()) {
            return simpleResponse(update, String.format("%s%n%s", CONFIG_TEXT, activeSettings(user)));
        }
        var matcher = CONFIG_PATTERN.matcher(payload);
        if (!matcher.matches()) {
            return simpleResponse(update, String.format("%s%n%s", "Wrong input!", CONFIG_TEXT));
        }
        try {
            updateSettings(user, matcher);
            return simpleResponse(update, String.format("Config updated!\n\n%s", activeSettings(user)));
        } catch (NumberFormatException | ValidationException e) {
            return simpleResponse(update, String.format("%s%n%s", "Wrong input!", CONFIG_TEXT));
        } catch (Exception e) {
            LOGGER.warn("Error while updating user settings", e);
            return simpleResponse(update, String.format("%s%n%s", "Wrong input!", CONFIG_TEXT));
        }
    }

    private void updateSettings(User user, Matcher matcher) {
        var priceMin = Integer.parseInt(matcher.group(1));
        var priceMax = Integer.parseInt(matcher.group(2));
        var roomsMin = Integer.parseInt(matcher.group(3));
        var roomsMax = Integer.parseInt(matcher.group(4));
        var yearMin = Integer.parseInt(matcher.group(5));
        var floorMin = Integer.parseInt(matcher.group(6));
        var showWithFees = "yes".equals(matcher.group(7));

        user.setPriceMin(priceMin);
        user.setPriceMax(priceMax);
        user.setRoomsMin(roomsMin);
        user.setRoomsMax(roomsMax);
        user.setYearMin(yearMin);
        user.setFloorMin(floorMin);
        user.setShowWithFees(showWithFees);

        if (priceMin > priceMax) {
            throw new ValidationException("Min price can't be bigger than max price");
        }
        if (roomsMin > roomsMax) {
            throw new ValidationException("Min rooms can't be bigger than max rooms");
        }
        var results = validator.validate(user);
        if (!results.isEmpty()) {
            throw new ValidationException("Error while validating: " + results);
        }

        users.save(user);
    }

    private String activeSettings(User user) {
        // TODO what when nothing configured?
        var status = Boolean.TRUE.equals(user.getEnabled()) ? "enabled" : "disabled";
        var showWithFees = Boolean.TRUE.equals(user.getShowWithFees()) ? "yes" : "no";
        return String.format("*Your active settings:*\n" +
                        "» *Notifications:* %1$s\n" +
                        "» *Price:* %2$d-%3$d€\n" +
                        "» *Rooms:* %4$d-%5$d\n" +
                        "» *From construction year:* %6$d\n" +
                        "» *Min floor:* %7$d\n" +
                        "» *Show with extra fees:* %8$s\n" +
                        "Current config:\n" +
                        "`%9$s %2$d %3$d %4$d %5$d %6$d %7$d %8$s`",
                status,
                user.getPriceMin(), user.getPriceMax(),
                user.getRoomsMin(), user.getRoomsMax(),
                user.getYearMin(),
                user.getFloorMin(),
                showWithFees,
                command());
    }

    private SendMessage simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.Markdown)
                .disableWebPagePreview(false);
    }
}
