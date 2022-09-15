package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UpdateListener {
    private final CommandRecognizer commandRecognizer;
    private final CallbackRecognizer callbackRecognizer;
    private final UserRepo users;

    public UpdateListener(CommandRecognizer commandRecognizer, CallbackRecognizer callbackRecognizer, UserRepo users) {
        this.commandRecognizer = commandRecognizer;
        this.callbackRecognizer = callbackRecognizer;
        this.users = users;
    }

    public int process(TelegramBot bot, List<Update> updates) {
        ensureUsersInDb(updates);

        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null) {
                handleMessage(bot, update);
            } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                handleCallback(bot, update);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void handleMessage(TelegramBot bot, Update update) {
        var rawText = update.message().text();
        var handler = commandRecognizer.getHandler(rawText);
        if (handler == null) {
            return;
        }
        var payload = commandRecognizer.getPayload(rawText);
        var message = handler.handle(update, payload);

        bot.execute(message);
    }

    private void handleCallback(TelegramBot bot, Update update) {
        var callbackData = update.callbackQuery().data();
        var handler = callbackRecognizer.getHandler(callbackData);
        if (handler == null) {
            return;
        }
        var payload = callbackRecognizer.getPayload(callbackData);
        handler.handle(update, bot, payload);
    }

    private void ensureUsersInDb(List<Update> updates) {
        updates.stream()
                .filter(update -> update.message() != null || update.callbackQuery() != null)
                .map(update -> Optional.ofNullable(update.message()).orElseGet(() -> update.callbackQuery().message()))
                .filter(message -> message.chat() != null)
                .map(message -> message.chat().id())
                .distinct()
                .forEach(this::ensureUserInDb);
    }

    private void ensureUserInDb(Long telegramId) {
        var existingUser = users.findByTelegramId(telegramId);
        if (existingUser.isPresent()) {
            return;
        }
        var newUser = new User(telegramId);
        users.save(newUser);
    }
}
