package com.joklek.rentbot.bot;

import com.joklek.rentbot.bot.commands.CommandResponder;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class CommandRecognizer {
    private final Pattern commandPattern = Pattern.compile("^/(\\w)+");
    private final Map<String, CommandResponder> handlers;
    private final CommandResponder errorHandler;

    public CommandRecognizer(Map<String, CommandResponder> handlers, CommandResponder errorHandler) {
        this.handlers = handlers;
        this.errorHandler = errorHandler;
    }

    public CommandResponder getHandler(String rawCommand) {
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
