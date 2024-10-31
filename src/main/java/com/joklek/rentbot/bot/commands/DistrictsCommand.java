package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.providers.DistrictsPageProvider;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
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
    public List<SendMessage> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        var message = districtsPageProvider.getDistrictsPageText(user);
        InlineKeyboardMarkup content;
        if (!user.getFilterByDistrict()) {
            content = districtsPageProvider.showTurnedOffPage();
        } else {
            content = districtsPageProvider.showPagedDistricts(user, 0);
        }

        return List.of(inlineResponse(update, message, content));
    }
}
