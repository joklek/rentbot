package com.joklek.rentbot.bot.replies;

import com.joklek.rentbot.bot.callbacks.ConfigCallback;
import com.joklek.rentbot.bot.providers.ConfigInfoProvider;
import com.joklek.rentbot.entities.SentMessage;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.SentMessageRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component(ConstructionMinReplyResponder.KEY)
public class ConstructionMinReplyResponder implements ReplyResponder {
    public static final String KEY = "minConstruction";

    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");

    private final UserRepo users;
    private final SentMessageRepo sentMessages;
    private final ConfigInfoProvider configInfoProvider;
    private final Validator validator;

    public ConstructionMinReplyResponder(UserRepo users, SentMessageRepo sentMessages, ConfigInfoProvider configInfoProvider, Validator validator) {
        this.users = users;
        this.sentMessages = sentMessages;
        this.configInfoProvider = configInfoProvider;
        this.validator = validator;
    }

    @Override
    public void handle(Message message, TelegramBot bot) {
        var text = message.text();
        var oldConfigMessage = sentMessages.findFirstByChatIdAndTypeOrderByMessageIdDesc(message.chat().id(), ConfigCallback.NAME).orElse(null);
        var matcher = PRICE_PATTERN.matcher(text);
        var user = users.getByTelegramId(message.chat().id());

        if (!matcher.matches()) {
            var sendMessage = new SendMessage(message.chat().id(), "❌ Wrong input! Please enter a number");
            sendMessage.replyToMessageId(message.messageId());
            sendConfigMessage(message, bot, user, oldConfigMessage);
            bot.execute(sendMessage);
            return;
        }
        var match = matcher.group(1);
        var constructionYearMin = Integer.parseInt(match);

        user.setYearMin(constructionYearMin);
        var results = validator.validate(user);
        if (!results.isEmpty()) {
            var firstViolation = results.stream().findFirst().get();
            var errorMessage = firstViolation.getMessage();
            var errorDescription = String.format("❌ Construction year %s", errorMessage);

            var sendMessage = new SendMessage(message.chat().id(), errorDescription);
            sendMessage.replyToMessageId(message.messageId());
            sendConfigMessage(message, bot, user, oldConfigMessage);
            bot.execute(sendMessage);
            return;
        }

        users.save(user);

        sendConfigMessage(message, bot, user, oldConfigMessage);
    }

    private void sendConfigMessage(Message message, TelegramBot bot, User user, SentMessage oldConfigMessage) {
        var newConfigMessage = new SendMessage(message.chat().id(), configInfoProvider.activeSettings(user));
        newConfigMessage.replyMarkup(configInfoProvider.showConfigPage(user));
        newConfigMessage.parseMode(ParseMode.Markdown);
        bot.execute(newConfigMessage);
        if (oldConfigMessage != null) {
            var deleteOldConfigMessage = new DeleteMessage(oldConfigMessage.getChatId(), oldConfigMessage.getMessageId());
            bot.execute(deleteOldConfigMessage);
            sentMessages.delete(oldConfigMessage);
        }
    }
}
