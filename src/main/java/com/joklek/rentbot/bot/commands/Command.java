package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;

/*
TODO is this really a command? Maybe it's an action. Command itself could be configured in config.
 Then multiple commands could share actions and it would be more obvious what's controlled where
 */
public interface Command {
    String command();

    BaseRequest<?, ?> handle(Update update, String payload);

    default SendMessage simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.Markdown)
                .disableWebPagePreview(false);
    }
}
