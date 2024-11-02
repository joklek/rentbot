package com.joklek.rentbot.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartCommand implements Command {

    private static final String MESSAGE = """
            ButaiBot is a project intended to help find apartments for sale in Vilnius, Lithuania. Turn on scanning and adjust configuration using the /config command and wait until bot sends you new listings.
            If you want to filter listings by their districts (rajonai), type in /districts.
            
            **Fun fact** - if you are couple and looking for an apartment - create a group and add this bot. To start configuration, write /config.
            """;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public List<SendMessage> handle(Update update, String payload) {
        return simpleFinalResponse(update, MESSAGE);
    }
}
