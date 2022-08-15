package com.joklek.rentbot.bot.commands;

import org.springframework.stereotype.Component;

@Component
public class InfoCommandResponder extends StartCommandResponder {

    @Override
    public String command() {
        return "/info";
    }
}
