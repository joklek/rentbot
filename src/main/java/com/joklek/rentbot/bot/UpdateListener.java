package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateListener {
    private final CommandRecognizer commandRecognizer;
    private final UserRepo users;

    public UpdateListener(CommandRecognizer commandRecognizer, UserRepo users) {
        this.commandRecognizer = commandRecognizer;
        this.users = users;
    }

    public int process(TelegramBot bot, List<Update> updates) {
        ensureUsersInDb(updates);

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

    private void ensureUsersInDb(List<Update> updates) {
        updates.stream()
                .filter(update -> update.message() != null)
                .filter(update -> update.message().chat() != null)
                .map(update -> update.message().chat().id())
                .distinct()
                .forEach(this::ensureUserInDb);
    }

    private void ensureUserInDb(Long telegramId) {
        var existingUser = users.findByTelegramId(telegramId);
        if (existingUser.isPresent()) {
            return;
        }
        var newUser = new User();
        newUser.setTelegramId(telegramId);
        users.save(newUser);
    }
}
