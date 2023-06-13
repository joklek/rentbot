package com.joklek.rentbot.config;

import com.joklek.rentbot.bot.CommandRecognizer;
import com.joklek.rentbot.bot.commands.Command;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CommandsConfig {

    @Bean
    public CommandRecognizer defaultRecognizer(List<Command> commands) {
        var commandsMapped = commands.stream().collect(Collectors.toMap(Command::command, x -> x));
        Command errorsHandler = null;
        return new CommandRecognizer(commandsMapped, errorsHandler); // TODO error handler
    }
}
