package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.IntegrationTest;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

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
        var response = command.handle(update, emptyPayload);

        var expectedText = """
                
                
                🔄 *Filter by district*: no (/districts to configure)
                """;

        assertThat(response).hasSize(1);
        var message = response.get(0);
        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
        assertThat(message.getParameters()).hasEntrySatisfying("reply_markup", new Condition<>() {
            @Override
            public boolean matches(Object value) {
                return value instanceof InlineKeyboardMarkup;
            }
        });
    }

    @Test
    void handle__whenNormalPayloadAndFirstTime__hasExpectedTextAndChangesConfig() {
        var response = command.handle(update, "200 330 1 2 2000 2 yes");

        // Has expected text
        var expectedText = """
                Config updated!
                
                📊 You would've seen 0 posts from last week with these settings.
                
                🔗 Share your settings with other people by sharing this command:
                ```
                /config 200 330 1 2 2000 2 yes
                ```
                
                🔄 *Filter by district*: no (/districts to configure)
                """;
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(0).getParameters()).hasEntrySatisfying("reply_markup", new Condition<>() {
            @Override
            public boolean matches(Object value) {
                return value instanceof InlineKeyboardMarkup;
            }
        });

        var user = users.findByTelegramId(CHAT_ID).get();
        // Auto enables user on first config change
        assertThat(user.getEnabled()).isTrue();
        // Changes user config
        assertThat(user.getPriceMin()).hasValueSatisfying(priceMin -> assertThat(priceMin).isEqualByComparingTo("200"));
        assertThat(user.getPriceMax()).hasValueSatisfying(priceMax -> assertThat(priceMax).isEqualByComparingTo("330"));
        assertThat(user.getRoomsMin()).hasValueSatisfying(roomsMin -> assertThat(roomsMin).isEqualTo(1));
        assertThat(user.getRoomsMax()).hasValueSatisfying(roomsMax -> assertThat(roomsMax).isEqualTo(2));
        assertThat(user.getYearMin()).hasValueSatisfying(yearMin -> assertThat(yearMin).isEqualTo(2000));
        assertThat(user.getFloorMin()).hasValueSatisfying(yearMin -> assertThat(yearMin).isEqualTo(2));
        assertThat(user.getShowWithFees()).isTrue();

        assertThat(user.getFilterByDistrict()).isFalse(); // This shouldn't change
    }

    @Test
    void handle__whenNormalPayloadAndNotFirstTime__hasExpectedTextAndChangesConfig() {
        makeUserAlreadyConfigured();
        var response = command.handle(update, "200 330 1 2 2000 2 yes");

        // Has expected text
        var expectedText = """
                Config updated!
                
                📊 You would've seen 0 posts from last week with these settings.
                
                🔗 Share your settings with other people by sharing this command:
                ```
                /config 200 330 1 2 2000 2 yes
                ```
                
                🔄 *Filter by district*: no (/districts to configure)
                """;
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(0).getParameters()).hasEntrySatisfying("reply_markup", new Condition<>() {
            @Override
            public boolean matches(Object value) {
                return value instanceof InlineKeyboardMarkup;
            }
        });

        var user = users.findByTelegramId(CHAT_ID).get();
        // Does not change notification flag
        assertThat(user.getEnabled()).isFalse();
        // Changes user config
        assertThat(user.getPriceMin()).hasValueSatisfying(priceMin -> assertThat(priceMin).isEqualByComparingTo("200"));
        assertThat(user.getPriceMax()).hasValueSatisfying(priceMax -> assertThat(priceMax).isEqualByComparingTo("330"));
        assertThat(user.getRoomsMin()).hasValueSatisfying(roomsMin -> assertThat(roomsMin).isEqualTo(1));
        assertThat(user.getRoomsMax()).hasValueSatisfying(roomsMax -> assertThat(roomsMax).isEqualTo(2));
        assertThat(user.getYearMin()).hasValueSatisfying(yearMin -> assertThat(yearMin).isEqualTo(2000));
        assertThat(user.getFloorMin()).hasValueSatisfying(yearMin -> assertThat(yearMin).isEqualTo(2));
        assertThat(user.getShowWithFees()).isTrue();

        assertThat(user.getFilterByDistrict()).isFalse(); // This shouldn't change
    }

    @Test
    void handle__whenNotInterestedInListingsWithFees__hasPropertyCorrectlySet() {
        var response = command.handle(update, "200 330 1 2 2000 2 no");

        // Has expected text
        var expectedText = """
                Config updated!
                
                📊 You would've seen 0 posts from last week with these settings.
                
                🔗 Share your settings with other people by sharing this command:
                ```
                /config 200 330 1 2 2000 2 no
                ```
                
                🔄 *Filter by district*: no (/districts to configure)
                """;
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(0).getParameters()).hasEntrySatisfying("reply_markup", new Condition<>() {
            @Override
            public boolean matches(Object value) {
                return value instanceof InlineKeyboardMarkup;
            }
        });

        var user = users.findByTelegramId(CHAT_ID).get();
        assertThat(user.getShowWithFees()).isFalse();
    }

    @Test
    void handle__whenInvalidPayloadPattern__returnsGenericErrorMessage() {
        var response = command.handle(update, "f 330 1 2 2000 2");

        // Has expected text
        var expectedText = """
                Wrong input!
                Use this format to configure your settings:

                ```
                /config <price_from> <price_to> <rooms_from> <rooms_to> <year_from> <min_floor> <show with fee?(yes/no)>
                ```
                Here's how your message might look like:
                ```
                /config 200 330 1 2 2000 2 yes

                ```Here you'd search for flats between 200 and 330 eur, 1-2 rooms, built after 2000, starting on the second floor, and you're ok with seeing listings with agency fees
                """;
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
    }

    private static Stream<Arguments> invalidPayloadsAndMessages() {
        return Stream.of(
                Arguments.of("500 100 1 2 2000 2 yes", "Min price can't be bigger than max price"),
                Arguments.of("100 500 3 1 2000 2 yes", "Min rooms can't be bigger than max rooms"),
                Arguments.of("99001 100002 1 2 2000 2 yes", "There's an error in priceMax: must be less than or equal to 100000, but was 100002."),
                Arguments.of("100 100001 1 2 2000 2 yes", "There's an error in priceMax: must be less than or equal to 100000, but was 100001."),
                Arguments.of("100 200 99 101 2000 2 yes", "There's an error in roomsMax: must be less than or equal to 100, but was 101."),
                Arguments.of("100 200 1 2 0 2 yes", "There's an error in yearMin: must be greater than or equal to 1000, but was 0."),
                Arguments.of("100 200 1 2 5000 2 yes", "There's an error in yearMin: must be less than or equal to 3000, but was 5000."),
                Arguments.of("100 200 1 2 2013 101 yes", "There's an error in floorMin: must be less than or equal to 100, but was 101.")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPayloadsAndMessages")
    void handle__whenInvalidConfigOptions__returnsSpecificErrorMessage(String payload, String message) {
        var response = command.handle(update, payload);

        // Has expected text
        var expectedText = String.format("""
                Wrong input! %s
                Use this format to configure your settings:

                ```
                /config <price_from> <price_to> <rooms_from> <rooms_to> <year_from> <min_floor> <show with fee?(yes/no)>
                ```
                Here's how your message might look like:
                ```
                /config 200 330 1 2 2000 2 yes

                ```Here you'd search for flats between 200 and 330 eur, 1-2 rooms, built after 2000, starting on the second floor, and you're ok with seeing listings with agency fees
                """, message);
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
    }

    private void makeUserAlreadyConfigured() {
        command.handle(update, "100 500 3 4 1900 1 no");
        var user = users.findByTelegramId(CHAT_ID).get();
        user.setEnabled(false);
        users.save(user);
    }
}