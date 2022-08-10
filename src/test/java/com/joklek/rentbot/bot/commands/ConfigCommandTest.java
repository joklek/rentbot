package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.IntegrationTest;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ConfigCommandTest extends IntegrationTest {

    @Autowired
    private ConfigCommand command;

    @Autowired
    private UserRepo users;
    @Mock
    private Update update;
    @Mock
    private Message message;
    @Mock
    private Chat chat;
    private static final Long CHAT_ID = 9999L;

    @BeforeEach
    void setUp() {
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(CHAT_ID);
        ensureUserInDb(CHAT_ID);
    }

    private void ensureUserInDb(Long chatId) {
        users.save(new User(chatId));
    }

    @Test
    void command() {
        assertThat(command.command()).isEqualTo("/config");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void handle__whenEmptyPayload(String emptyPayload) {
        BaseRequest<?, ?> response = command.handle(update, emptyPayload);

        var expectedText = """
                Use this format:

                ```
                /config <price_from> <price_to> <rooms_from> <rooms_to> <year_from> <min_flor> <show with fee?(yes/no)>
                ```
                Example:
                ```
                /config 200 330 1 2 2000 2 yes
                ```
                *Your active settings:*
                » *Notifications:* disabled
                » *Price:* 0-0€
                » *Rooms:* 0-0
                » *From construction year:* 0
                » *Min floor:* 0
                » *Show with extra fees:* no
                Current config:
                `/config 0 0 0 0 0 0 no`
                """;
        assertThat(response.getParameters()).containsEntry("text", expectedText);
    }
}