package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.callbacks.DistrictsCallback;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.DistrictRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DistrictsCommand implements Command {

    private final UserRepo users;
    private final DistrictRepo districts;

    public DistrictsCommand(UserRepo users, DistrictRepo districts) {
        this.users = users;
        this.districts = districts;
    }

    @Override
    public String command() {
        return "/districts";
    }

    @Override
    public List<BaseRequest<?, ?>> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        String message;
        InlineKeyboardMarkup content;
        if (!user.getFilterByDistrict()) {
            message = "There is a possibility to filter listings by district. Listings without any district will always be shown. Please note that some sites have different district classifications or names.";
            content = showTurnedOffPage();
        } else {
            message = "Please select your wanted districts. If none are selected all listings will be shown. Listings without any district will always be shown. Please note that some sites have different district classifications or names.";
            content = showPagedDistricts(user, 0);
        }
        return List.of(inlineResponse(update, message, content));
    }

    private InlineKeyboardMarkup showTurnedOffPage() {
        var keyboard = new InlineKeyboardMarkup();
        var turnOnButton = new InlineKeyboardButton("✅ Turn on");
        turnOnButton.callbackData(DistrictsCallback.TurnOn.CALLBACK_KEY);
        keyboard.addRow(turnOnButton);
        return keyboard;
    }

    private InlineKeyboardMarkup showPagedDistricts(User user, int page) {
        var keyboard = new InlineKeyboardMarkup();
        var allDistricts = districts.findAllByOrderByNameAsc();
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
            districtButton.callbackData(DistrictsCallback.Toggle.callbackKey(district.getId()));
            return districtButton;
        }).toList();
        var secondRowButtons = secondRowDistricts.stream().map(district -> {
            var name = userDistricts.contains(district) ? String.format("✅%s", district.getName()) : district.getName();
            var districtButton = new InlineKeyboardButton(name);
            districtButton.callbackData(DistrictsCallback.Toggle.callbackKey(district.getId()));
            return districtButton;
        }).toList();

        var prevButton = new InlineKeyboardButton("⬅");
        prevButton.callbackData(DistrictsCallback.Page.callbackKey(prevPage));
        var nextButton = new InlineKeyboardButton("➡");
        nextButton.callbackData(DistrictsCallback.Page.callbackKey(nextPage));
        var resetButton = new InlineKeyboardButton("\uD83D\uDD04");
        resetButton.callbackData(DistrictsCallback.Reset.CALLBACK_KEY);
        var turnOffButton = new InlineKeyboardButton("❌");
        turnOffButton.callbackData(DistrictsCallback.TurnOff.CALLBACK_KEY);
        keyboard.addRow(firstRowButtons.toArray(InlineKeyboardButton[]::new));
        keyboard.addRow(secondRowButtons.toArray(InlineKeyboardButton[]::new));
        keyboard.addRow(prevButton, nextButton, resetButton, turnOffButton); // control row
        return keyboard;
    }

    private SendMessage inlineResponse(Update update, String message, InlineKeyboardMarkup content) {
        return new SendMessage(update.message().chat().id(), message)
                .replyMarkup(content)
                .parseMode(ParseMode.Markdown); // TODO migrate to V2 markdown and see why it don't work
    }
}
