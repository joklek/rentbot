package com.joklek.rentbot;

import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.PostEntityConverter;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.joklek.rentbot.scraper.AruodasScraper;
import com.joklek.rentbot.scraper.PostDto;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.function.Predicate.not;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ScheduledScraper {

    private static final Logger LOGGER = getLogger(ScheduledScraper.class);

    private final List<AruodasScraper> scrapers;
    private final UserRepo users;
    private final PostRepo posts;
    private final PostEntityConverter postConverter;
    private final TelegramBot bot;

    public ScheduledScraper(List<AruodasScraper> scrapers, UserRepo users, PostRepo posts, PostEntityConverter postConverter, TelegramBot bot) {
        this.scrapers = scrapers;
        this.users = users;
        this.posts = posts;
        this.postConverter = postConverter;
        this.bot = bot;
    }

    @Scheduled(fixedRate = 5, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void scrapePosts() {
        var shuffledScrapers = new ArrayList<>(scrapers);
        Collections.shuffle(shuffledScrapers);

        var unpublishedPosts = shuffledScrapers.stream()
                .map(AruodasScraper::getLatestPosts)
                .flatMap(Collection::stream)
                .filter(not(post -> posts.existsByExternalIdAndSource(post.getExternalId(), post.getSource())))
                .toList();

        unpublishedPosts.stream()
                .map(post -> save(post))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(post -> notifyUsers(post));
    }

    private Optional<Post> save(PostDto postDto) {
        var post = postConverter.convert(postDto);
        try {
            return Optional.of(posts.save(post));
        } catch (Exception e) {
            LOGGER.error("Failed to save post", e);
            return Optional.empty();
        }
    }

    private void notifyUsers(Post post) {
        if (post.getPrice().isEmpty()) {
            return;
        }

        getInterestedTelegramIDs(post)
                .forEach(telegramId -> {
                    try {
                        sendTelegramMessage(telegramId, post);
                    } catch (Exception e) {
                        LOGGER.error("Can't send telegram message to {}", telegramId, e);
                    }
                });
    }

    private List<Long> getInterestedTelegramIDs(Post post) {
        if (post.getWithFees()) {
            return users.findAllInterestedTelegramIdsWithFee(post.getPrice().orElse(null), post.getRooms().orElse(null), post.getConstructionYear().orElse(null), post.getFloor().orElse(null));
        }
        return users.findAllInterestedTelegramIds(post.getPrice().orElse(null), post.getRooms().orElse(null), post.getConstructionYear().orElse(null), post.getFloor().orElse(null));
    }

    private void sendTelegramMessage(Long telegramId, Post post) {
        var sb = new StringBuilder();

        sb.append(String.format("%d. %s\n", post.getId(), post.getLink()));

        post.getPhone().ifPresent(phone -> sb.append(String.format("» *Phone number:* [%1$s](tel:%1$s)\n", phone)));

        var address = getAddress(post);
        if (address.isPresent()) {
            sb.append(String.format("» *Address:* [%s](https://maps.google.com/?q=%s)\n", address.get(), URLEncoder.encode(address.get(), StandardCharsets.UTF_8)));
        }

        if (post.getPrice().isPresent() && post.getArea().isPresent()) {
            sb.append(String.format("» *Price:* `%.2f€ (%.2f€/m²)`\n", post.getPrice().get(), post.getPrice().get().divide(post.getArea().get(), RoundingMode.UP)));
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
