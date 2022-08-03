package com.joklek.rentbot.bot;

import com.joklek.rentbot.bot.commands.Command;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class CommandRecognizer {
    private final Pattern commandPattern = Pattern.compile("^/(\\w)+");
    private final Map<String, Command> handlers;
    private final Command errorHandler;

    public CommandRecognizer(Map<String, Command> handlers, Command errorHandler) {
        this.handlers = handlers;
        this.errorHandler = errorHandler;
    }

    public Command getHandler(String rawCommand) {
        var matcher = commandPattern.matcher(rawCommand);
        if (!matcher.find()) {
            return errorHandler;
        }
        var extractedCommand = matcher.group();
        var command = handlers.get(extractedCommand);
        return Optional.ofNullable(command)
                .orElse(errorHandler);
    }

    public String getPayload(String rawCommand) {
        var matcher = commandPattern.matcher(rawCommand);
        if (!matcher.find()) {
            return null;
        }
        var extractedCommand = matcher.group();
        return rawCommand.substring(extractedCommand.length()).strip();
    }
}
