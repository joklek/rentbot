package com.joklek.rentbot.bot;

import com.joklek.rentbot.bot.callbacks.CallbackResponder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CallbackRecognizer {
    private final Pattern commandPattern = Pattern.compile("^/f(\\w)+:");
    private final Map<String, CallbackResponder> handlers;

    public CallbackRecognizer(Map<String, CallbackResponder> handlers) {
        this.handlers = handlers;
    }

    public CallbackResponder getHandler(String rawCommand) {
        var matcher = commandPattern.matcher(rawCommand);
        if (!matcher.find()) {
            return null;
        }
        var extractedCommand = matcher.group()
                .replaceFirst("/f", "")
                .replaceFirst(":", "");
        return handlers.get(extractedCommand);
    }

    public String[] getPayload(String rawCommand) {
        var matcher = commandPattern.matcher(rawCommand);
        if (!matcher.find()) {
            return new String[0];
        }
        var extractedCommand = matcher.group();
        return rawCommand.substring(extractedCommand.length()).strip().split(":");
    }
}
