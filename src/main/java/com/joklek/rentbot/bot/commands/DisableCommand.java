package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class DisableCommand implements Command {
    private final UserRepo users;

    public DisableCommand(UserRepo users) {
        this.users = users;
    }

    @Override
    public String command() {
        return "/disable";
    }

    @Override
    @Transactional
    public BaseRequest<?, ?> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        user.setEnabled(false);

        return simpleResponse(update, "Notifications disabled!");
    }
}
