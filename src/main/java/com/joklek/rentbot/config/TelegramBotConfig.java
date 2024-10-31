package com.joklek.rentbot.config;

import com.joklek.rentbot.bot.UpdateListener;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetMyName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBot defaultBot(@Value("${application.telegram.token}") String token, UpdateListener updateListener) {
        var bot = new TelegramBot.Builder(token).build();
        var botName = bot.execute(new GetMyName()).botName().name();
        updateListener.setBotName(botName);
        bot.setUpdatesListener(updates -> updateListener.process(bot, updates));

        return bot;
    }
}
