package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.Post;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostResponseCreatorTest {
    private final PostResponseCreator postResponseCreator = new PostResponseCreator();
    private final String link = "https://example.com";
    private final Long telegramId = 123L;
    private final BigDecimal price = new BigDecimal("123.45");
    private final int rooms = 3;
    private final int constructionYear = 1990;
    private final int floor = 2;
    private final int totalFloors = 5;
    private final String street = "Street";
    private final String district = "District";
    private final String houseNumber = "123";
    private final String heating = "Hot";
    private final BigDecimal area = new BigDecimal("45.67");
    private final String buildingState = "Good";
    private final String buildingMaterial = "Brick";

    @Test
    void createTelegramMessage() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setTotalFloors(totalFloors);
        post.setStreet(street);
        post.setDistrict(district);
        post.setHouseNumber(houseNumber);
        post.setHeating(heating);
        post.setArea(area);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [District Street 123](https://maps.google.com/?q=District+Street+123)
                » *Price:* `123.45€ (2.70€/m²)`
                » *Rooms:* `3 (45.67m²)`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2/5`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenNoAreaInPost__thenAreaPerSqmIsNotCalculated() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setTotalFloors(totalFloors);
        post.setStreet(street);
        post.setDistrict(district);
        post.setHouseNumber(houseNumber);
        post.setHeating(heating);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [District Street 123](https://maps.google.com/?q=District+Street+123)
                » *Price:* `123.45€`
                » *Rooms:* `3`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2/5`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenTotalFloorsMissing() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setStreet(street);
        post.setDistrict(district);
        post.setHouseNumber(houseNumber);
        post.setHeating(heating);
        post.setArea(area);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [District Street 123](https://maps.google.com/?q=District+Street+123)
                » *Price:* `123.45€ (2.70€/m²)`
                » *Rooms:* `3 (45.67m²)`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenNoDistrictInPost__thenAddressIsWithoutDistrict() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setTotalFloors(totalFloors);
        post.setStreet(street);
        post.setHouseNumber(houseNumber);
        post.setHeating(heating);
        post.setArea(area);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [Street 123](https://maps.google.com/?q=Street+123)
                » *Price:* `123.45€ (2.70€/m²)`
                » *Rooms:* `3 (45.67m²)`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2/5`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenNoStreetInPost__thenAddressIsWithoutStreet() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setTotalFloors(totalFloors);
        post.setDistrict(district);
        post.setHouseNumber(houseNumber);
        post.setHeating(heating);
        post.setArea(area);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [District](https://maps.google.com/?q=District)
                » *Price:* `123.45€ (2.70€/m²)`
                » *Rooms:* `3 (45.67m²)`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2/5`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenNoHouseNumberInPost__thenAddressIsWithoutHouseNumber() {
        var post = new Post();
        post.setLink(link);
        post.setPrice(price);
        post.setRooms(rooms);
        post.setConstructionYear(constructionYear);
        post.setFloor(floor);
        post.setTotalFloors(totalFloors);
        post.setStreet(street);
        post.setDistrict(district);
        post.setHeating(heating);
        post.setArea(area);
        post.setBuildingState(buildingState);
        post.setBuildingMaterial(buildingMaterial);
        var expectedText = """
                null. https://example.com
                » *Address:* [District Street](https://maps.google.com/?q=District+Street)
                » *Price:* `123.45€ (2.70€/m²)`
                » *Rooms:* `3 (45.67m²)`
                » *Contruction year:* `1990`
                » *Heating type:* `Hot`
                » *Floor:* `2/5`
                » *Building material:* `Brick`
                » *Building state:* `Good`
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }

    @Test
    void createTelegramMessage__whenPostIsEmpty__postsEmptyMessage() {
        var post = new Post();
        post.setLink(link);
        var expectedText = """
                null. https://example.com
                """;

        var message = postResponseCreator.createTelegramMessage(telegramId, List.of(post));

        assertThat(message.getParameters()).containsEntry("text", expectedText);
        assertThat(message.getParameters()).containsEntry("parse_mode", "Markdown");
    }
}