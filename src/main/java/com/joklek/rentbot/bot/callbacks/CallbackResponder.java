package com.joklek.rentbot.bot.callbacks;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;

public interface CallbackResponder {
    default String command() {
        return String.format("f%s", name());
    }

    String name();

    BaseRequest<?, ?> handle(Update update, TelegramBot bot, String... payload);

    default SendMessage simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.Markdown) // TODO migrate to V2 markdown and see why it don't work
                .disableWebPagePreview(false);
    }
}
