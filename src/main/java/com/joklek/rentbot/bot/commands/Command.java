package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.List;

public interface Command {
    String command();

    List<BaseRequest<?, ?>> handle(Update update, String payload);

    default BaseRequest<?, ?> simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.Markdown) // TODO migrate to V2 markdown and see why it don't work
                .disableWebPagePreview(false);
    }

    default List<BaseRequest<?, ?>> simpleFinalResponse(Update update, String message) {
        return List.of(simpleResponse(update, message));
    }
}
