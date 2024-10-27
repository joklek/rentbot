package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.IntegrationTest;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DistrictsCommandTest extends IntegrationTest {

    @Autowired
    private DistrictsCommand command;

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
        assertThat(command.command()).isEqualTo("/districts");
    }

    @Test
    void handle__whenFilteringByDistrictOff__thenGetTurnedOffPage() {
        var expectedText = "There is a possibility to filter listings by district. Listings without any district will always be shown. Please note that some sites have different district classifications or names.\n\nDon't forget to turn on scanning in /config";

        var response = command.handle(update, "");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(0).getParameters()).containsKey("reply_markup");
        assertThat(response.get(0).getParameters().get("reply_markup")).isInstanceOf(InlineKeyboardMarkup.class);
        var replyMarkup = (InlineKeyboardMarkup) response.get(0).getParameters().get("reply_markup");
        assertThat(replyMarkup.inlineKeyboard())
                .hasDimensions(1, 1);
        assertThat(replyMarkup.inlineKeyboard()[0][0])
                .isInstanceOf(InlineKeyboardButton.class);
        assertThat(replyMarkup.inlineKeyboard()[0][0].text())
                .isEqualTo("✅ Turn on");
        assertThat(replyMarkup.inlineKeyboard()[0][0].callbackData())
                .isEqualTo("/fdistricts:on");
    }

    @Test
    void handle__whenFilteringByDistrictOn__thenGetsFirstPage() {
        var user = users.findByTelegramId(CHAT_ID).get();
        user.setFilterByDistrict(true);
        users.save(user);
        var expectedText = "Please select your wanted districts. If none are selected all listings will be shown. Listings without any district will always be shown. Please note that some sites have different district classifications or names.\n\nDon't forget to turn on scanning in /config";

        var response = command.handle(update, "");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(0).getParameters()).containsKey("reply_markup");
        assertThat(response.get(0).getParameters().get("reply_markup")).isInstanceOf(InlineKeyboardMarkup.class);
        var replyMarkup = (InlineKeyboardMarkup) response.get(0).getParameters().get("reply_markup");
        assertThat(replyMarkup.inlineKeyboard()).hasNumberOfRows(5);

        // First row of districts
        assertThat(replyMarkup.inlineKeyboard()[0]).hasSize(3)
                .allSatisfy(button -> assertThat(button.callbackData()).matches("/fdistricts:toggle:\\d+"))
                .allSatisfy(button -> assertThat(button.text()).isNotBlank());

        // Second row of districts
        assertThat(replyMarkup.inlineKeyboard()[1]).hasSize(3)
                .allSatisfy(button -> assertThat(button.callbackData()).matches("/fdistricts:toggle:\\d+"))
                .allSatisfy(button -> assertThat(button.text()).isNotBlank());

        // Third row of districts
        assertThat(replyMarkup.inlineKeyboard()[2]).hasSize(3)
                .allSatisfy(button -> assertThat(button.callbackData()).matches("/fdistricts:toggle:\\d+"))
                .allSatisfy(button -> assertThat(button.text()).isNotBlank());

        // Control row is present
        assertThat(replyMarkup.inlineKeyboard()[3]).hasSize(3); // Control row
        assertThat(replyMarkup.inlineKeyboard()[3][0].text()).isEqualTo("⬅");
        assertThat(replyMarkup.inlineKeyboard()[3][0].callbackData()).isEqualTo("/fdistricts:page:0");
        assertThat(replyMarkup.inlineKeyboard()[3][1].text()).isEqualTo("➡");
        assertThat(replyMarkup.inlineKeyboard()[3][1].callbackData()).isEqualTo("/fdistricts:page:1");
        assertThat(replyMarkup.inlineKeyboard()[3][2].text()).isEqualTo("Reset");
        assertThat(replyMarkup.inlineKeyboard()[3][2].callbackData()).isEqualTo("/fdistricts:reset");

        // Last for filtering by district
        assertThat(replyMarkup.inlineKeyboard()[4]).hasSize(1);
        assertThat(replyMarkup.inlineKeyboard()[4][0].text()).isEqualTo("Remove filtering by district");
        assertThat(replyMarkup.inlineKeyboard()[4][0].callbackData()).isEqualTo("/fdistricts:off");
    }
}