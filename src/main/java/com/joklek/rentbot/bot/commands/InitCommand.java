package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class InitCommand implements Command {

    private final UserRepo users;

    public InitCommand(UserRepo users) {
        this.users = users;
    }

    @Override
    public String command() {
        return "/init";
    }

    @Override
    @Transactional
    public BaseRequest<?, ?> handle(Update update, String payload) {

        var telegramId = update.message().chat().id();
        var user = getUser(telegramId);
        if (isReady(user)) {
            user.setEnabled(true);

            return simpleResponse(update, "yeye");
        } else {
            return simpleResponse(update, "uhoh");
        }
    }

    private User getUser(Long telegramId) {
        var existingUser = users.findByTelegramId(telegramId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        var newUser = new User();
        newUser.setTelegramId(telegramId);
        newUser.setEnabled(false);
        return newUser; // TODO
//        return users.save(newUser);
    }

    private boolean isReady(User user) {
        return true;
//        return nonNull(user.getPriceMin()) && nonNull(user.getPriceMax());
    }

    private SendMessage simpleResponse(Update update, String uhoh) {
        return new SendMessage(update.message().chat().id(), uhoh)
                .parseMode(ParseMode.MarkdownV2)
                .disableWebPagePreview(false);
    }
}
