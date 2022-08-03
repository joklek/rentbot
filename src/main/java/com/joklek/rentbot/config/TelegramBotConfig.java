package com.joklek.rentbot.config;

import com.joklek.rentbot.bot.CommandRecognizer;
import com.joklek.rentbot.bot.UpdateListener;
import com.joklek.rentbot.bot.commands.Command;
import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBot defaultBot(@Value("${application.telegram.token}") String token, UpdateListener updateListener) {
        var bot = new TelegramBot.Builder(token).build();
        bot.setUpdatesListener(updates -> updateListener.process(bot, updates));

        return bot;
    }

    @Bean
    public CommandRecognizer defaultRecognizer(List<Command> commands) {
        var commandsMapped = commands.stream().collect(Collectors.toMap(Command::command, x -> x));
        Command errorsHandler = null;
        return new CommandRecognizer(commandsMapped, errorsHandler); // TODO error handler
    }
}
