package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ReplayCommandResponder implements CommandResponder {
    private static final Logger LOGGER = getLogger(ReplayCommandResponder.class);

    private static final int POSTS_FROM_PREVIOUS_DAYS = 2;

    private final PostRepo posts;
    private final UserRepo users;
    private final TelegramBot bot;

    public ReplayCommandResponder(PostRepo posts, UserRepo users, TelegramBot bot) {
        this.posts = posts;
        this.users = users;
        this.bot = bot;
    }

    @Override
    public String command() {
        return "/replay";
    }

    @Override
    public BaseRequest<?, ?> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if(!user.isConfigured()) {
            return simpleResponse(update, "Please configure your settings with /config");
        }

        var posts = getPosts(user);

        if (posts.isEmpty()) {
            return simpleResponse(update, String.format("No posts found in the last %d days", POSTS_FROM_PREVIOUS_DAYS));
        }

        for (Post post : posts) {
            try {
                sendTelegramMessage(telegramId, post);
            } catch (Exception e) {
                LOGGER.error("Can't send telegram message to {}", telegramId, e);
            }
        }

        return simpleResponse(update, String.format("Replayed %d posts from last %d days", posts.size(), POSTS_FROM_PREVIOUS_DAYS));
    }

    private List<Post> getPosts(User user) {
        return posts.getAllPostsForUserFromDays(
                user.getId(),
                LocalDateTime.now().minusDays(POSTS_FROM_PREVIOUS_DAYS)
        );
    }

    // TODO MOVE TO COMMON PLACE FROM HERE AND SCHEDULED
    private void sendTelegramMessage(Long telegramId, Post post) {
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
        } else if (post.getRooms().isPresent()) {
            sb.append(String.format("» *Floor:* `%d`\n", post.getFloor().get()));
        }

        if (post.getWithFees()) {
            sb.append("» *With fee:* yes\n");
        } else {
            sb.append("» *With fee:* no\n");
        }

        bot.execute(new SendMessage(telegramId, sb.toString())
                .parseMode(ParseMode.Markdown) // TODO migrate to V2 markdown and see why it don't work
                .disableWebPagePreview(false));
        // TODO use internal bot, not this?
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
