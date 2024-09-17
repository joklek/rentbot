package com.joklek.rentbot.bot.replies;

import com.joklek.rentbot.bot.callbacks.ConfigCallback;
import com.joklek.rentbot.bot.providers.ConfigInfoProvider;
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

        var matcher = PRICE_PATTERN.matcher(text);

        if (!matcher.matches()) {
            var sendMessage = new SendMessage(message.chat().id(), "Wrong input! Please enter a number.");
            sendMessage.replyToMessageId(message.messageId());
            bot.execute(sendMessage);
            return;
        }
        var match = matcher.group(1);
        var constructionYearMin = Integer.parseInt(match);

        var user = users.getByTelegramId(message.chat().id());
        user.setYearMin(constructionYearMin);
        var results = validator.validate(user);
        if (!results.isEmpty()) {
            var firstViolation = results.stream().findFirst().get();
            var errorPath = firstViolation.getPropertyPath().toString();
            var errorMessage = firstViolation.getMessage();
            var badValue = firstViolation.getInvalidValue();
            var errorDescription = String.format("There's an error in %s: %s, but was %s.", errorPath, errorMessage, badValue);

            var sendMessage = new SendMessage(message.chat().id(), errorDescription);
            sendMessage.replyToMessageId(message.messageId());
            bot.execute(sendMessage);
            return;
        }

        users.save(user);

        var configMessage = sentMessages.findFirstByChatIdAndTypeOrderByMessageIdDesc(message.chat().id(), ConfigCallback.NAME);

        var newConfigMessage = new SendMessage(message.chat().id(), configInfoProvider.activeSettings(user));
        newConfigMessage.replyMarkup(configInfoProvider.showConfigPage(user));
        newConfigMessage.parseMode(ParseMode.Markdown);

        bot.execute(newConfigMessage);

        if (configMessage.isPresent()) {
            var lastConfigMessage = configMessage.get();
            var deleteOldConfigMessage = new DeleteMessage(lastConfigMessage.getChatId(), lastConfigMessage.getMessageId());
            bot.execute(deleteOldConfigMessage);
        }
    }
}
