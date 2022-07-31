package com.joklek.rentbot;

import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.joklek.rentbot.scraper.AruodasScraper;
import com.joklek.rentbot.scraper.PostDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledScraper {

    private final List<AruodasScraper> scrapers;
    private final UserRepo users;
    private final PostRepo posts;

    public ScheduledScraper(List<AruodasScraper> scrapers, UserRepo users, PostRepo posts) {
        this.scrapers = scrapers;
        this.users = users;
        this.posts = posts;
    }

    @Scheduled(fixedRateString = "2", timeUnit = TimeUnit.MINUTES)
    public void scrapePosts() {
        var shuffledScrapers = new ArrayList<>(scrapers);
        Collections.shuffle(shuffledScrapers);

        var unpublishedPosts = shuffledScrapers.stream()
                .map(AruodasScraper::getLatestPosts)
                .flatMap(Collection::stream)
//                .filter(post -> posts.existsByExternalIdAndSource(post.getExternalId(), post.getSource()))  // TODO filter by internal id and provider if not exists
                .toList();

        unpublishedPosts.stream()
                .map(post -> save(post))
                .forEach(post -> notifyUsers(post));
    }

    private Post save(PostDto post) {
        // TODO
        return null;
    }

    private void notifyUsers(Post post) {
        var users = getInterestedTelegramIDs(post);
        users.forEach(user -> sendTelegramMessage(user, post));
    }

    private List<User> getInterestedTelegramIDs(Post post) {
        // TODO
        return List.of();
    }

    private void sendTelegramMessage(User user, Post post) {
        // TODO
    }
}
