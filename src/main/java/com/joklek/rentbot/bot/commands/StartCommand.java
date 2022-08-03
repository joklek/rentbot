package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {

    private static final String MESSAGE
            = "BBTMV-noRestrict - 'Butų NE TIK Be Tarpininkavimo Mokesčio Vilniuje' is a project intended to help find flats for a rent in Vilnius, Lithuania. All you have to do is to set config using /config command and wait until bot sends you notifications.\n\n**Fun fact** - if you are couple and looking for a flat, then create group chat and add this bot into that group - enable settings and bot will send notifications to the same chat. :)";

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public BaseRequest<?, ?> handle(Update update, String payload) {
        return simpleResponse(update, MESSAGE);
    }

    private SendMessage simpleResponse(Update update, String message) {
        return new SendMessage(update.message().chat().id(), message)
                .parseMode(ParseMode.MarkdownV2)
                .disableWebPagePreview(false);
    }
}
