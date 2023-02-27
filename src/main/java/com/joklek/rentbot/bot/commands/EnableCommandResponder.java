package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class EnableCommandResponder implements CommandResponder {
    private final UserRepo users;
    private final Validator validator;

    public EnableCommandResponder(UserRepo users, Validator validator) {
        this.users = users;
        this.validator = validator;
    }

    @Override
    public String command() {
        return "/enable";
    }

    @Override
    @Transactional
    public BaseRequest<?, ?> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if (!validator.validate(user).isEmpty()) {
            return simpleResponse(update, "You must first use /config command before using /enable or /disable commands!");
        }

        user.setEnabled(true);
        return simpleResponse(update, "Notifications enabled!");
    }
}
