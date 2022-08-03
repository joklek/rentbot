package com.joklek.rentbot.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateListener {
    private final CommandRecognizer commandRecognizer;

    public UpdateListener(CommandRecognizer commandRecognizer) {
        this.commandRecognizer = commandRecognizer;
    }

    public int process(TelegramBot bot, List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() == null) {
                return;
            }
            var rawText = update.message().text();
            var handler = commandRecognizer.getHandler(rawText);
            var payload = commandRecognizer.getPayload(rawText);
            var message = handler.handle(update, payload);

            bot.execute(message);
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
