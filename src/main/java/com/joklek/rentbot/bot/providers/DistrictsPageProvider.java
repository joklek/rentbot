package com.joklek.rentbot.bot.providers;

import com.joklek.rentbot.bot.callbacks.DistrictsCallback;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.DistrictRepo;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.springframework.stereotype.Component;

@Component
public class DistrictsPageProvider {
    private final DistrictRepo districts;

    public DistrictsPageProvider(DistrictRepo districts) {
        this.districts = districts;
    }

    public InlineKeyboardMarkup showTurnedOffPage() {
        var keyboard = new InlineKeyboardMarkup();
        var turnOnButton = new InlineKeyboardButton("✅ Turn on");
        turnOnButton.callbackData(DistrictsCallback.TurnOn.CALLBACK_KEY);
        keyboard.addRow(turnOnButton);
        return keyboard;
    }

    public InlineKeyboardMarkup showPagedDistricts(User user, int page) {
        var keyboard = new InlineKeyboardMarkup();
        var allDistricts = districts.findAllByOrderByNameAsc();
        var userDistricts = districts.findByUsers(user);
        var pageSize = 9;
        var rowSize = 3;
        var pageCount = allDistricts.size() / pageSize;
        var nextPage = Math.min(page + 1, pageCount);
        var prevPage = Math.max(page - 1, 0);

        var fromIndex = page * pageSize;

        var firstRowDistricts = allDistricts.stream().skip(fromIndex).limit(rowSize).toList();
        var secondRowDistricts = allDistricts.stream().skip(fromIndex + rowSize).limit(rowSize).toList();
        var thirdRowDistricts = allDistricts.stream().skip(fromIndex + rowSize + rowSize).limit(rowSize).toList();

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
        var thirdRowButtons = thirdRowDistricts.stream().map(district -> {
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
        keyboard.addRow(thirdRowButtons.toArray(InlineKeyboardButton[]::new));
        keyboard.addRow(prevButton, nextButton, resetButton, turnOffButton); // control row
        return keyboard;
    }
}
