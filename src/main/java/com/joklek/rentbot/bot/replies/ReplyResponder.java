package com.joklek.rentbot.bot.replies;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;

public interface ReplyResponder {
    void handle(Message message, TelegramBot bot);
}
