package com.joklek.rentbot;

import com.joklek.rentbot.bot.PostResponseCreator;
import com.joklek.rentbot.bot.providers.PostDeduplicator;
import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.PostEntityConverter;
import com.joklek.rentbot.entities.PostPriceHistory;
import com.joklek.rentbot.repo.DistrictRepo;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.joklek.rentbot.scraper.PostDto;
import com.joklek.rentbot.scraper.Scraper;
import com.pengrad.telegrambot.TelegramBot;
import org.slf4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static org.slf4j.LoggerFactory.getLogger;

@Profile("!test")
@Component
public class ScheduledScraper {

    private static final Logger LOGGER = getLogger(ScheduledScraper.class);

    private final List<Scraper> scrapers;
    private final UserRepo users;
    private final PostRepo posts;
    private final DistrictRepo districts;
    private final PostEntityConverter postConverter;
    private final PostResponseCreator postResponseCreator;
    private final PostDeduplicator postDeduplicator;
    private final TelegramBot bot;
    private final Random random;

    public ScheduledScraper(List<Scraper> scrapers, UserRepo users, PostRepo posts, DistrictRepo districts, PostEntityConverter postConverter, PostResponseCreator postResponseCreator, PostDeduplicator postDeduplicator, TelegramBot bot) {
        this.scrapers = scrapers;
        this.users = users;
        this.posts = posts;
        this.districts = districts;
        this.postConverter = postConverter;
        this.postResponseCreator = postResponseCreator;
        this.postDeduplicator = postDeduplicator;
        this.bot = bot;
        this.random = new Random();
    }

    @Scheduled(fixedRate = 15, initialDelay = 0L, timeUnit = TimeUnit.MINUTES)
    public void scrapePosts() throws InterruptedException {
        if (isNightTime()) {
            return;
        } else {
            Thread.sleep(SECONDS.toMillis(random.nextInt(60)));
        }

        var shuffledScrapers = new ArrayList<>(scrapers);
        Collections.shuffle(shuffledScrapers);

        var unpublishedPosts = shuffledScrapers.stream() // TODO parallel streams?
                .map(ScheduledScraper::getLatestPosts)
                .flatMap(Collection::stream)
                .toList();

        var posts = unpublishedPosts.stream()
                .map(post -> save(post))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<List<Post>> deduplicatedPosts = postDeduplicator.deduplicatePosts(posts);

        deduplicatedPosts.forEach(similarPosts -> notifyUsers(similarPosts));
    }

    private static List<PostDto> getLatestPosts(Scraper scraper) {
        try {
            return scraper.getLatestPosts();
        } catch (Exception e) {
            LOGGER.error("{} failed with", scraper.getClass(), e);
            return List.of();
        }
    }

    private boolean isNightTime() {
        var now = LocalDateTime.now();
        return now.getHour() >= 23 || now.getHour() < 8;
    }

    private Optional<Post> save(PostDto postDto) {
        var maybePost = fetchPost(postDto);
        if (maybePost.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(posts.save(maybePost.get()));
        } catch (Exception e) {
            LOGGER.error("Failed to save post", e);
            return Optional.empty();
        }
    }

    private Optional<Post> fetchPost(PostDto postDto) {
        var foundPost = posts.findByExternalIdAndSource(postDto.getExternalId(), postDto.getSource());
        if (foundPost.isEmpty() || postDto.getPrice().isEmpty()) {
            if (postDto.isPartial()) {
                LOGGER.warn("Partial post found, skipping. ID: {}", postDto.getExternalId());
                return Optional.empty();
            }
            return Optional.of(postConverter.convert(postDto));
        }

        var post = foundPost.get();
        var newPrice = postDto.getPrice().get();
        if (post.getPrice().isPresent() && post.getPrice().get().compareTo(newPrice) == 0) {
            return Optional.empty();
        }

        LOGGER.info("Price changed for post with ID: {}. Old price: {}. New price: {}", post.getId(), post.getPrice().orElse(BigDecimal.ZERO), newPrice);
        post.setPrice(newPrice);
        post.addPostPriceHistory(new PostPriceHistory(newPrice));
        return Optional.of(post);
    }

    private void notifyUsers(List<Post> posts) {
        posts.forEach(post -> logPost(post));
        if (posts.stream().allMatch(post -> post.getPrice().isEmpty())) {
            return;
        }

        posts.stream().flatMap(duplicatePost -> getInterestedTelegramIds(duplicatePost).stream())
                .distinct()
                .forEach(telegramId -> {
                    try {
                        bot.execute(postResponseCreator.createTelegramMessage(telegramId, posts));
                    } catch (Exception e) {
                        LOGGER.error("Can't send telegram message to {}", telegramId, e);
                    }
                });
    }

    private List<Long> getInterestedTelegramIds(Post post) {
        if (post.getDistrict().isEmpty() || !districts.existsByName(post.getDistrict().get())) {
            return getDistrictlessInterestedTelegramIds(post);
        }
        return getInterestedWithDistrict(post);
    }

    private List<Long> getDistrictlessInterestedTelegramIds(Post post) {
        var price = post.getPrice().orElse(null);
        var rooms = post.getRooms().orElse(null);
        var year = post.getConstructionYear().orElse(null);
        var floor = post.getFloor().orElse(null);
        var area = post.getArea().map(a -> a.toBigInteger().intValue()).orElse(null);

        return users.findAllTelegramIdsInterested(price, rooms, year, floor, area);
    }

    private List<Long> getInterestedWithDistrict(Post post) {
        var interestedInDistrict = findAllTelegramIdsInterestedInDistrict(post);
        var districtless = findAllTelegramIdsNotInterestedInDistricts(post);
        return Stream.concat(interestedInDistrict.stream(), districtless.stream()).toList();
    }

    private List<Long> findAllTelegramIdsInterestedInDistrict(Post post) {
        var price = post.getPrice().orElse(null);
        var rooms = post.getRooms().orElse(null);
        var year = post.getConstructionYear().orElse(null);
        var floor = post.getFloor().orElse(null);
        var district = post.getDistrict().orElse(null);
        var area = post.getArea().map(a -> a.toBigInteger().intValue()).orElse(null);

        return users.findAllTelegramIdsInterestedInDistrict(price, rooms, year, floor, area, district);
    }

    private List<Long> findAllTelegramIdsNotInterestedInDistricts(Post post) {
        var price = post.getPrice().orElse(null);
        var rooms = post.getRooms().orElse(null);
        var year = post.getConstructionYear().orElse(null);
        var floor = post.getFloor().orElse(null);
        var area = post.getArea().map(a -> a.toBigInteger().intValue()).orElse(null);

        return users.findAllTelegramIdsNotInterestedInDistricts(price, rooms, year, floor, area);
    }

    private void logPost(Post post) {
        LOGGER.info("ID:{} Desc:{} Dist:{} Addr:{} Heat:{} Fl:{} FlTot:{} Area:{} Price:{} Room:{} Year:{} State:{} Material:{} Link:{}",
                post.getId(), post.getDescriptionHash().isPresent(),
                post.getDistrict().map(String::length).orElse(null), post.getStreet().map(String::length).orElse(null),
                post.getHeating().map(String::length).orElse(null), post.getFloor().orElse(null), post.getTotalFloors().orElse(null),
                post.getArea().map(area -> String.format("%.2f", area)).orElse(null), post.getPrice().map(price -> String.format("%.2f", price)).orElse(null),
                post.getRooms().orElse(null), post.getConstructionYear().orElse(null),
                post.getBuildingState().isPresent(), post.getBuildingMaterial().isPresent(), post.getLink());
    }
}
