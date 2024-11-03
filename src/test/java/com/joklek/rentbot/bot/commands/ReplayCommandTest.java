package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.IntegrationTest;
import com.joklek.rentbot.entities.District;
import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.PostPriceHistory;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.DistrictRepo;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ReplayCommandTest extends IntegrationTest {

    @Autowired
    private ReplayCommand command;
    @Autowired
    private UserRepo users;
    @Autowired
    private PostRepo posts;
    @Autowired
    private DistrictRepo districts;

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

    private User makeUserAlreadyConfigured(Long chatId) {
        var user = users.findByTelegramId(chatId).get();
        user.setPriceMin(BigDecimal.valueOf(100));
        user.setPriceMax(BigDecimal.valueOf(500));
        user.setRoomsMin(3);
        user.setRoomsMax(4);
        user.setYearMin(1900);
        user.setFloorMin(1);
        user.setAreaMin(1);
        user.setEnabled(true);
        return users.save(user);
    }

    @Test
    void command() {
        assertThat(command.command()).isEqualTo("/replay");
    }

    @Test
    void handle__whenUserNotConfigured__notifiesUser() {
        var expectedText = "Please configure your settings with /config";
        var response = command.handle(update, "");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
    }

    @Test
    void handle__whenNoPosts__notifiesUser() {
        makeUserAlreadyConfigured(CHAT_ID);
        var expectedText = "No listings found in the last 2 days";
        var response = command.handle(update, "");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
    }

    @Test
    void handle__whenPostsExistAndNoDistrictsConfigured__thenSendsPostAndNotification() {
        makeUserAlreadyConfigured(CHAT_ID);
        var post = createPost(LocalDateTime.now().minusMinutes(2L));
        var expectedText = String.format("""
                %d. EXTERNAL_LINK
                » *Address:* [Random](https://maps.google.com/?q=Random)
                » *Price:* `150.00€`
                » *Rooms:* `3`
                » *Contruction year:* `1999`
                » *Floor:* `2`
                """, post.getId());
        var response = command.handle(update, "");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(1).getParameters()).containsEntry("text", "Replayed 1 listings from last 2 days");
    }

    @Test
    void handle__whenPostsExistAndDistrictsConfigured__thenSendsPostAndNotification() {
        var district = new District();
        district.setName("Test District");
        districts.save(district);
        var user = makeUserAlreadyConfigured(CHAT_ID);
        user.setFilterByDistrict(true);
        user.getDistricts().add(district);
        users.save(user);

        var postInterested = createPost(LocalDateTime.now().minusMinutes(2L), district.getName());
        var expectedText = String.format("""
                %d. EXTERNAL_LINK
                » *Address:* [Test District](https://maps.google.com/?q=Test+District)
                » *Price:* `150.00€`
                » *Rooms:* `3`
                » *Contruction year:* `1999`
                » *Floor:* `2`
                """, postInterested.getId());
        var response = command.handle(update, "");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(1).getParameters()).containsEntry("text", "Replayed 1 listings from last 2 days");
    }
    @Test
    void handle__whenPostsWithNotExistingDistrictExistsAndDistrictsConfigured__thenSendsPostAndNotification() {
        var district = new District();
        district.setName("Test District");
        districts.save(district);
        var user = makeUserAlreadyConfigured(CHAT_ID);
        user.setFilterByDistrict(true);
        user.getDistricts().add(district);
        users.save(user);

        var postInterested = createPost(LocalDateTime.now().minusMinutes(2L), "ASDFW");
        var expectedText = String.format("""
                %d. EXTERNAL_LINK
                » *Address:* [ASDFW](https://maps.google.com/?q=ASDFW)
                » *Price:* `150.00€`
                » *Rooms:* `3`
                » *Contruction year:* `1999`
                » *Floor:* `2`
                """, postInterested.getId());
        var response = command.handle(update, "");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(1).getParameters()).containsEntry("text", "Replayed 1 listings from last 2 days");
    }

    @Test
    void handle__whenOnlyInvalidPostsExist__thenSendsNotification() {
        makeUserAlreadyConfigured(CHAT_ID);
        createPost(LocalDateTime.now().minusDays(2L).minusMinutes(2L));
        var response = command.handle(update, "");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getParameters()).containsEntry("text", "No listings found in the last 2 days");
    }

    @Test
    void handle__whenOldPostButPriceUpdatedRecently__thenSendsNotification() {
        makeUserAlreadyConfigured(CHAT_ID);
        var post = createPost(LocalDateTime.now().minusDays(2L).minusMinutes(2L));
        var postPriceHistory = new PostPriceHistory(BigDecimal.valueOf(200));
        post.addPostPriceHistory(postPriceHistory);
        post.setPrice(postPriceHistory.getPrice());
        posts.save(post);
        var expectedText = String.format("""
                %d. EXTERNAL_LINK
                » *Address:* [Random](https://maps.google.com/?q=Random)
                » *Price:* `200.00€`
                » *Rooms:* `3`
                » *Contruction year:* `1999`
                » *Floor:* `2`
                """, post.getId());
        var response = command.handle(update, "");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getParameters()).containsEntry("text", expectedText);
        assertThat(response.get(1).getParameters()).containsEntry("text", "Replayed 1 listings from last 2 days");
    }

    private Post createPost(LocalDateTime createdAt, String district) {
        var post = new Post();
        var price = BigDecimal.valueOf(150);
        post.setPrice(price);
        post.setRooms(3);
        post.setConstructionYear(1999);
        post.setFloor(2);
        post.setExternalId("EXTERNAL");
        post.setSource("EXTERNAL_SOURCE");
        post.setLink("EXTERNAL_LINK");
        post.setDistrict(district);
        post.setCreatedAt(createdAt);
        var postPriceHistory = new PostPriceHistory(price);
        postPriceHistory.setCreatedAt(createdAt);
        post.addPostPriceHistory(postPriceHistory);
        return posts.save(post);
    }

    private Post createPost(LocalDateTime createdAt) {
        return createPost(createdAt, "Random");
    }
}