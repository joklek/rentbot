package com.joklek.rentbot.bot;

import com.joklek.rentbot.bot.replies.ReplyResponder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ReplyRecognizer {
    private final Map<String, ReplyResponder> handlers;

    public ReplyRecognizer(Map<String, ReplyResponder> handlers) {
        this.handlers = handlers;
    }

    public Optional<ReplyResponder> getHandler(String type) {
        return Optional.ofNullable(handlers.get(type));
    }
}
