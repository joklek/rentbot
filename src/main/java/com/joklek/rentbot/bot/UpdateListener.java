package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.SentMessage;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.SentMessageRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class UpdateListener {
    private static final Logger LOGGER = getLogger(UpdateListener.class);

    private final CommandRecognizer commandRecognizer;
    private final CallbackRecognizer callbackRecognizer;
    private final ReplyRecognizer replyRecognizer;
    private final UserRepo users;
    private final SentMessageRepo replyableMessages;

    public UpdateListener(CommandRecognizer commandRecognizer, CallbackRecognizer callbackRecognizer, ReplyRecognizer replyRecognizer, UserRepo users, SentMessageRepo replyableMessages) {
        this.commandRecognizer = commandRecognizer;
        this.callbackRecognizer = callbackRecognizer;
        this.replyRecognizer = replyRecognizer;
        this.users = users;
        this.replyableMessages = replyableMessages;
    }

    public int process(TelegramBot bot, List<Update> updates) {
        ensureUsersInDb(updates);

        updates.forEach(update -> {
            if (update.message() != null && update.message().text() != null && update.message().text().startsWith("/")) {
                handleMessage(bot, update);
            } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                handleCallback(bot, update);
            } else if (update.message().replyToMessage() != null) {
                handleReply(bot, update);
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
        var messages = handler.handle(update, payload);

        messages.forEach(request -> bot.execute(request, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                if (!response.isOk()) {
                    LOGGER.error("Failed to send message: {}", response.description());
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                LOGGER.error("Failed to send message", e);
            }
        }));
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

    private void handleReply(TelegramBot bot, Update update) {
        var maybeType = replyableMessages.findByChatIdAndMessageId(update.message().chat().id(), update.message().replyToMessage().messageId())
                .map(SentMessage::getType);
        if (maybeType.isEmpty()) {
            LOGGER.warn("Message not found in db");
            return;
        }
        var type = maybeType.get();
        replyRecognizer.getHandler(type).ifPresent(handler -> handler.handle(update.message(), bot));
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
