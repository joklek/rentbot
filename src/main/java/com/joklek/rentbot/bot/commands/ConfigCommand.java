package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.providers.ConfigInfoProvider;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ConfigCommand implements Command {

    private static final Logger LOGGER = getLogger(ConfigCommand.class);

    private static final Pattern CONFIG_PATTERN = Pattern.compile("^(\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (yes|no)$");
    private static final String CONFIG_TEXT = "Use this format to configure your settings:\n\n```\n/config <price_from> <price_to> <rooms_from> <rooms_to> <min_area> <year_from> <min_floor> <show with fee?(yes/no)>\n```\nHere's how your message might look like:\n```\n/config 200 330 1 2 50 2000 2 yes\n\n```Here you'd search for flats between 200 and 330 eur, 1-2 rooms, total area is 50m², built after 2000, starting on the second floor, and you're ok with seeing listings with agency fees\n";

    private final UserRepo users;
    private final Validator validator;
    private final ConfigInfoProvider configInfoProvider;

    public ConfigCommand(UserRepo users, Validator validator, ConfigInfoProvider configInfoProvider) {
        this.users = users;
        this.validator = validator;
        this.configInfoProvider = configInfoProvider;
    }

    @Override
    public String command() {
        return "/config";
    }

    @Override
    public List<SendMessage> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if (payload.isBlank()) {
            return List.of(inlineResponse(update, configInfoProvider.activeSettings(user), configInfoProvider.showConfigPage(user)));
        }
        var matcher = CONFIG_PATTERN.matcher(payload);
        if (!matcher.matches()) {
            return simpleFinalResponse(update, String.format("%s\n%s", "Wrong input!", CONFIG_TEXT));
        }
        try {
            updateSettings(user, matcher);
            return List.of(inlineResponse(update, String.format("Config updated!\n\n%s", configInfoProvider.activeSettings(user)), configInfoProvider.showConfigPage(user)));
        } catch (NumberFormatException e) {
            return simpleFinalResponse(update, String.format("%s\n%s", "Wrong input! Check if all numbers are written correctly", CONFIG_TEXT));
        } catch (ConstraintViolationException e) {
            var firstViolation = e.getConstraintViolations().stream().findFirst().get();
            var errorPath = firstViolation.getPropertyPath().toString();
            var errorMessage = firstViolation.getMessage();
            var badValue = firstViolation.getInvalidValue();
            var errorDescription = String.format("There's an error in %s: %s, but was %s.", errorPath, errorMessage, badValue);
            return simpleFinalResponse(update, String.format("%s %s\n%s", "Wrong input!", errorDescription, CONFIG_TEXT));
        } catch (ValidationException e) {
            return simpleFinalResponse(update, String.format("%s %s\n%s", "Wrong input!", e.getMessage(), CONFIG_TEXT));
        } catch (Exception e) {
            LOGGER.warn("Error while updating user settings", e);
            return simpleFinalResponse(update, String.format("%s\n%s", "Wrong input!", CONFIG_TEXT));
        }
    }

    private void updateSettings(User user, Matcher matcher) {
        var isFirstTimeSettingUp = !user.isConfigured();

        var priceMin = new BigDecimal(matcher.group(1));
        var priceMax = new BigDecimal(matcher.group(2));
        var roomsMin = Integer.parseInt(matcher.group(3));
        var roomsMax = Integer.parseInt(matcher.group(4));
        var areaMin = Integer.parseInt(matcher.group(5));
        var yearMin = Integer.parseInt(matcher.group(6));
        var floorMin = Integer.parseInt(matcher.group(7));
        var showWithFees = "yes".equals(matcher.group(8));

        user.setPriceMin(priceMin);
        user.setPriceMax(priceMax);
        user.setRoomsMin(roomsMin);
        user.setRoomsMax(roomsMax);
        user.setAreaMin(areaMin);
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
}
