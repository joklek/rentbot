package com.joklek.rentbot.config;

import com.joklek.rentbot.bot.CommandRecognizer;
import com.joklek.rentbot.bot.commands.CommandResponder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CommandsConfig {

    @Bean
    public CommandRecognizer defaultRecognizer(List<CommandResponder> commandResponders) {
        var commandsMapped = commandResponders.stream().collect(Collectors.toMap(CommandResponder::command, x -> x));
        CommandResponder errorsHandler = null;
        return new CommandRecognizer(commandsMapped, errorsHandler); // TODO error handler
    }
}
