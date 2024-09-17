package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.providers.DistrictsPageProvider;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DistrictsCommand implements Command {

    private final UserRepo users;
    private final DistrictsPageProvider districtsPageProvider;

    public DistrictsCommand(UserRepo users, DistrictsPageProvider districtsPageProvider) {
        this.users = users;
        this.districtsPageProvider = districtsPageProvider;
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
            content = districtsPageProvider.showTurnedOffPage();
        } else {
            message = "Please select your wanted districts. If none are selected all listings will be shown. Listings without any district will always be shown. Please note that some sites have different district classifications or names.";
            content = districtsPageProvider.showPagedDistricts(user, 0);
        }
        return List.of(inlineResponse(update, message, content));
    }
}
