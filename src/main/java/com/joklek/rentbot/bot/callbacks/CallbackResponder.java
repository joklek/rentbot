package com.joklek.rentbot.bot.callbacks;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;

public interface CallbackResponder {
    default String command() {
        return String.format("f%s", name());
    }

    String name();

    BaseRequest<?, ?> handle(Update update, TelegramBot bot, String... payload);
}
