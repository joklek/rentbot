package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.Post;
import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class PostResponseCreator {

    public SendMessage createTelegramMessage(Long telegramId, List<Post> posts) {
        var sb = new StringBuilder();
        posts.forEach(post -> sb.append(String.format("%d. %s\n", post.getId(), post.getLink())));

        var post = getPostForRepresentation(posts);
        var address = getAddress(post);
        if (address.isPresent()) {
            sb.append(String.format("» *Address:* [%s](https://maps.google.com/?q=%s)\n", address.get(), URLEncoder.encode(address.get(), StandardCharsets.UTF_8)));
        }

        if (post.getPrice().isPresent() && post.getArea().isPresent()) {
            sb.append(String.format("» *Price:* `%.2f€ (%.2f€/m²)`\n", post.getPrice().get(), post.getPrice().get().divide(post.getArea().get(), 2, RoundingMode.HALF_EVEN)));
        } else if (post.getPrice().isPresent()) {
            sb.append(String.format("» *Price:* `%.2f€`\n", post.getPrice().get()));
        }

        if (post.getRooms().isPresent() && post.getArea().isPresent()) {
            sb.append(String.format("» *Rooms:* `%d (%.2fm²)`\n", post.getRooms().get(), post.getArea().get()));
        } else if (post.getRooms().isPresent()) {
            sb.append(String.format("» *Rooms:* `%d`\n", post.getRooms().get()));
        }

        if (post.getConstructionYear().isPresent()) {
            sb.append(String.format("» *Contruction year:* `%d`\n", post.getConstructionYear().get()));
        }

        if (post.getHeating().isPresent()) {
            sb.append(String.format("» *Heating type:* `%s`\n", post.getHeating().get()));
        }

        if (post.getFloor().isPresent() && post.getTotalFloors().isPresent()) {
            sb.append(String.format("» *Floor:* `%d/%d`\n", post.getFloor().get(), post.getTotalFloors().get()));
        } else if (post.getFloor().isPresent()) {
            sb.append(String.format("» *Floor:* `%d`\n", post.getFloor().get()));
        }

        if (post.getBuildingMaterial().isPresent()) {
            sb.append(String.format("» *Building material:* `%s`\n", post.getBuildingMaterial().get()));
        }

        if (post.getBuildingState().isPresent()) {
            sb.append(String.format("» *Building state:* `%s`\n", post.getBuildingState().get()));
        }

        return new SendMessage(telegramId, sb.toString())
                .parseMode(ParseMode.Markdown)
                .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true));
    }

    /**
     * Given a list of posts which are determined to be the same, return a single post which represents all of them.
     * The new post is not saved into the database. It is assumed that the list of posts has the newest posts first.
     */
    private Post getPostForRepresentation(List<Post> posts) {
        var post = new Post();

        var price = posts.stream().map(Post::getPrice).filter(Optional::isPresent).map(Optional::get).findFirst();
        price.ifPresent(post::setPrice);

        var rooms = posts.stream().map(Post::getRooms).filter(Optional::isPresent).map(Optional::get).findFirst();
        rooms.ifPresent(post::setRooms);

        var constructionYear = posts.stream().map(Post::getConstructionYear).filter(Optional::isPresent).map(Optional::get).findFirst();
        constructionYear.ifPresent(post::setConstructionYear);

        var floor = posts.stream().map(Post::getFloor).filter(Optional::isPresent).map(Optional::get).findFirst();
        floor.ifPresent(post::setFloor);

        var totalFloors = posts.stream().map(Post::getTotalFloors).filter(Optional::isPresent).map(Optional::get).findFirst();
        totalFloors.ifPresent(post::setTotalFloors);

        var street = posts.stream().map(Post::getStreet).filter(Optional::isPresent).map(Optional::get).findFirst();
        street.ifPresent(post::setStreet);

        var district = posts.stream().map(Post::getDistrict).filter(Optional::isPresent).map(Optional::get).findFirst();
        district.ifPresent(post::setDistrict);

        var houseNumber = posts.stream().map(Post::getHouseNumber).filter(Optional::isPresent).map(Optional::get).findFirst();
        houseNumber.ifPresent(post::setHouseNumber);

        var heating = posts.stream().map(Post::getHeating).filter(Optional::isPresent).map(Optional::get).findFirst();
        heating.ifPresent(post::setHeating);

        var area = posts.stream().map(Post::getArea).filter(Optional::isPresent).map(Optional::get).findFirst();
        area.ifPresent(post::setArea);

        var buildingState = posts.stream().map(Post::getBuildingState).filter(Optional::isPresent).map(Optional::get).findFirst();
        buildingState.ifPresent(post::setBuildingState);

        var buildingMaterial = posts.stream().map(Post::getBuildingMaterial).filter(Optional::isPresent).map(Optional::get).findFirst();
        buildingMaterial.ifPresent(post::setBuildingMaterial);

        return post;
    }

    private Optional<String> getAddress(Post post) {
        var sb = new StringBuilder();
        if (post.getDistrict().isPresent()) {
            sb.append(post.getDistrict().get()).append(" ");
        }
        if (post.getStreet().isPresent()) {
            sb.append(post.getStreet().get()).append(" ");
            if (post.getHouseNumber().isPresent()) {
                sb.append(post.getHouseNumber().get());
            }
        }
        if (sb.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sb.toString().trim());
    }
}
