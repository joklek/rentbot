package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.IntegrationTest;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EnableCommandTest extends IntegrationTest {

    @Autowired
    private EnableCommand command;
    @Autowired
    private ConfigCommand configCommand;

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
        assertThat(command.command()).isEqualTo("/enable");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Random text", ""})
    void handle__whenUserAlreadyEnabled__notificationIsReceived(String payload) {
        makeUserAlreadyConfigured(true);
        var response = command.handle(update, payload);

        var user = users.findByTelegramId(CHAT_ID).get();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", "Notifications enabled!");
        assertThat(user.getEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Random text", ""})
    void handle__whenUserDisabled__enablesUser(String payload) {
        makeUserAlreadyConfigured(false);
        var response = command.handle(update, payload);

        var user = users.findByTelegramId(CHAT_ID).get();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", "Notifications enabled!");
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    void handle__whenUserUnconfigured__returnsError() {
        var response = command.handle(update, "");

        var user = users.findByTelegramId(CHAT_ID).get();
        // Auto enables user on first config change
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", "You must first use /config command before using /enable or /disable commands!");
        assertThat(user.getEnabled()).isFalse();
    }

    private void makeUserAlreadyConfigured(boolean isEnabled) {
        configCommand.handle(update, "100 500 3 4 1900 1 no");
        var user = users.findByTelegramId(CHAT_ID).get();
        user.setEnabled(isEnabled);
        users.save(user);
    }
}