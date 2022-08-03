package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;

public interface Command {
    String command();

    BaseRequest<?, ?> handle(Update update, String payload);
}
