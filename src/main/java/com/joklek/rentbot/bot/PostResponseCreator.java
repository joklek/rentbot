package com.joklek.rentbot.bot;

import com.joklek.rentbot.entities.Post;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class PostResponseCreator {

    public SendMessage createTelegramMessage(Long telegramId, Post post) {
        var sb = new StringBuilder();

        sb.append(String.format("%d. %s\n", post.getId(), post.getLink()));

        post.getPhone().ifPresent(phone -> sb.append(String.format("» *Phone number:* [%1$s](tel:%1$s)\n", phone)));

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

        if (post.getWithFees()) {
            sb.append("» *With fee:* yes\n");
        } else {
            sb.append("» *With fee:* no\n");
        }

        return new SendMessage(telegramId, sb.toString())
                .parseMode(ParseMode.Markdown) // TODO migrate to V2 markdown and see why it don't work
                .disableWebPagePreview(false);
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
