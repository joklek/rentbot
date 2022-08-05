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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.function.Predicate.not;

@Component
public class ScheduledScraper {

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
                .forEach(post -> notifyUsers(post));
    }

    private Post save(PostDto postDto) {
        var post = postConverter.convert(postDto);
        return posts.save(post);
    }

    private void notifyUsers(Post post) {
        getInterestedTelegramIDs(post)
                .forEach(telegramId -> sendTelegramMessage(telegramId, post));
    }

    private List<Long> getInterestedTelegramIDs(Post post) {
        // TODO with fees
        return users.findAllInterestedTelegramIds(post.getPrice(), post.getRooms(), post.getConstructionYear(), post.getFloor());
    }

    private void sendTelegramMessage(Long telegramId, Post post) {
        var sb = new StringBuilder();

        sb.append(String.format("%d. %s\n", post.getId(), post.getLink()));

        if (post.getPhone() != null) {
            sb.append(String.format("» *Phone number:* [%1$s](tel:%1$s)\n", post.getPhone()));
        }

        var address = getAddress(post);
        if (address != null) {
            sb.append(String.format("» *Address:* [%s](https://maps.google.com/?q=%s)\n", address, URLEncoder.encode(address, StandardCharsets.UTF_8)));
        }

        if (post.getPrice() != null && post.getArea() != null) {
            sb.append(String.format("» *Price:* `%.2f€ (%.2f€/m²)`\n", post.getPrice(), post.getPrice().divide(post.getArea(), RoundingMode.UP)));
        } else if (post.getPrice() != null) {
            sb.append(String.format("» *Price:* `%.2f€`\n", post.getPrice()));
        }

        if (post.getRooms() != null && post.getArea() != null) {
            sb.append(String.format("» *Rooms:* `%d (%.2fm²)`\n", post.getRooms(), post.getArea()));
        } else if (post.getRooms() != null) {
            sb.append(String.format("» *Rooms:* `%d`\n", post.getRooms()));
        }

        if (post.getConstructionYear() != null) {
            sb.append(String.format("» *Contruction year:* `%d`\n", post.getConstructionYear()));
        }

        if (post.getHeating() != null) {
            sb.append(String.format("» *Heating type:* `%s`\n", post.getHeating()));
        }

        if (post.getFloor() != null && post.getTotalFloors() != null) {
            sb.append(String.format("» *Floor:* `%d/%d`\n", post.getFloor(), post.getTotalFloors()));
        } else if (post.getRooms() != null) {
            sb.append(String.format("» *Floor:* `%d`\n", post.getFloor()));
        }

        if (Boolean.TRUE.equals(post.getWithFees())) {
            sb.append("» *With fee:* yes\n");
        } else {
            sb.append("» *With fee:* no\n");
        }

        bot.execute(new SendMessage(telegramId, sb.toString())
                .parseMode(ParseMode.Markdown)
                .disableWebPagePreview(false));
        // TODO use internal bot, not this?
    }

    private String getAddress(Post post) {
        var sb = new StringBuilder();
        if (post.getDistrict() != null) {
            sb.append(post.getDistrict()).append(" ");
        }
        if (post.getStreet() != null) {
            sb.append(post.getStreet()).append(" ");
            if (post.getHouseNumber() != null) {
                sb.append(post.getHouseNumber());
            }
        }
        if (sb.isEmpty()) {
            return null;
        }
        return sb.toString().trim();
    }
}
