package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnableCommand implements Command {
    private final UserRepo users;
    private final Validator validator;

    public EnableCommand(UserRepo users, Validator validator) {
        this.users = users;
        this.validator = validator;
    }

    @Override
    public String command() {
        return "/enable";
    }

    @Override
    @Transactional
    public List<BaseRequest<?, ?>> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if (!validator.validate(user).isEmpty()) {
            return simpleFinalResponse(update, "You must first use /config command before using /enable or /disable commands!");
        }

        user.setEnabled(true);
        return simpleFinalResponse(update, "Notifications enabled!");
    }
}
