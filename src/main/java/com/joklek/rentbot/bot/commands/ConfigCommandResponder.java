package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ConfigCommandResponder implements CommandResponder {

    private static final Logger LOGGER = getLogger(ConfigCommandResponder.class);

    private static final Pattern CONFIG_PATTERN = Pattern.compile("^(\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (yes|no)$");
    private static final String CONFIG_TEXT = "Use this format to configure your settings:\n\n```\n/config <price_from> <price_to> <rooms_from> <rooms_to> <year_from> <min_floor> <show with fee?(yes/no)>\n```\nHere's how your message might look like:\n```\n/config 200 330 1 2 2000 2 yes\n\n```Here you'd search for flats between 200 and 330 eur, 1-2 rooms, built after 2000, starting on the second floor, and you're ok with seeing listings with agency fees\n";

    private final UserRepo users;
    private final Validator validator;

    public ConfigCommandResponder(UserRepo users, Validator validator) {
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
            return simpleResponse(update, String.format("%s\n%s", CONFIG_TEXT, activeSettings(user)));
        }
        var matcher = CONFIG_PATTERN.matcher(payload);
        if (!matcher.matches()) {
            return simpleResponse(update, String.format("%s\n%s", "Wrong input!", CONFIG_TEXT));
        }
        try {
            updateSettings(user, matcher);
            return simpleResponse(update, String.format("Config updated!\n\n%s", activeSettings(user)));
        } catch (NumberFormatException e) {
            return simpleResponse(update, String.format("%s\n%s", "Wrong input! Check if all numbers are written correctly", CONFIG_TEXT));
        } catch (ConstraintViolationException e) {
            var firstViolation = e.getConstraintViolations().stream().findFirst().get();
            var errorPath = firstViolation.getPropertyPath().toString();
            var errorMessage = firstViolation.getMessage();
            var badValue = firstViolation.getInvalidValue();
            var errorDescription = String.format("There's an error in %s: %s, but was %s.", errorPath, errorMessage, badValue);
            return simpleResponse(update, String.format("%s %s\n%s", "Wrong input!", errorDescription, CONFIG_TEXT));
        } catch (ValidationException e) {
            return simpleResponse(update, String.format("%s %s\n%s", "Wrong input!", e.getMessage(), CONFIG_TEXT));
        } catch (Exception e) {
            LOGGER.warn("Error while updating user settings", e);
            return simpleResponse(update, String.format("%s\n%s", "Wrong input!", CONFIG_TEXT));
        }
    }

    private void updateSettings(User user, Matcher matcher) {
        var isFirstTimeSettingUp = user.getPriceMin().isEmpty();

        var priceMin = new BigDecimal(matcher.group(1));
        var priceMax = new BigDecimal(matcher.group(2));
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
        if (isFirstTimeSettingUp) {
            user.setEnabled(true);
        }

        if (priceMin.compareTo(priceMax) > 0) {
            throw new ValidationException("Min price can't be bigger than max price");
        }
        if (roomsMin > roomsMax) {
            throw new ValidationException("Min rooms can't be bigger than max rooms");
        }
        var results = validator.validate(user);
        if (!results.isEmpty()) {
            throw new ConstraintViolationException(results);
        }

        users.save(user);
    }

    private String activeSettings(User user) {
        // TODO what when nothing configured?
        var status = user.getEnabled() ? "enabled" : "disabled";
        var showWithFees = user.getShowWithFees() ? "yes" : "no";
        var filterByDistrict = user.getFilterByDistrict() ? "yes" : "no";
        return String.format("*Your active settings:*\n" +
                        "» *Notifications:* %1$s\n" +
                        "» *Price:* %2$.0f-%3$.0f€\n" +
                        "» *Rooms:* %4$d-%5$d\n" +
                        "» *From construction year:* %6$d\n" +
                        "» *Min floor:* %7$d\n" +
                        "» *Show with extra fees:* %8$s\n" +
                        "» *Filter by district:* %9$s (/districts to configure)\n" +
                        "Current config:\n" +
                        "`%10$s %2$.0f %3$.0f %4$d %5$d %6$d %7$d %8$s`",
                status,
                user.getPriceMin().orElse(BigDecimal.ZERO), user.getPriceMax().orElse(BigDecimal.ZERO),
                user.getRoomsMin().orElse(0), user.getRoomsMax().orElse(0),
                user.getYearMin().orElse(0),
                user.getFloorMin().orElse(0),
                showWithFees,
                filterByDistrict,
                command());
    }
}
