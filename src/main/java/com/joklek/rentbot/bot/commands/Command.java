package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.List;

public interface Command {
    String command();

    List<BaseRequest<?, ?>> handle(Update update, String payload);

    default BaseRequest<?, ?> simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.Markdown)
                .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true));
    }

    default List<BaseRequest<?, ?>> simpleFinalResponse(Update update, String message) {
        return List.of(simpleResponse(update, message));
    }

    default SendMessage inlineResponse(Update update, String message, InlineKeyboardMarkup content) {
        return new SendMessage(update.message().chat().id(), message)
                .replyMarkup(content)
                .parseMode(ParseMode.Markdown);
    }
}
