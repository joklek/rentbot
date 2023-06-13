package com.joklek.rentbot.bot.commands;

import org.springframework.stereotype.Component;

@Component
public class InfoCommand extends StartCommand {

    @Override
    public String command() {
        return "/info";
    }
}
