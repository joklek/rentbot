package com.joklek.rentbot;

import com.joklek.rentbot.bot.PostResponseCreator;
import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.PostEntityConverter;
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
    private final TelegramBot bot;
    private final Random random;

    public ScheduledScraper(List<Scraper> scrapers, UserRepo users, PostRepo posts, DistrictRepo districts, PostEntityConverter postConverter, PostResponseCreator postResponseCreator, TelegramBot bot) {
        this.scrapers = scrapers;
        this.users = users;
        this.posts = posts;
        this.districts = districts;
        this.postConverter = postConverter;
        this.postResponseCreator = postResponseCreator;
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
                .map(Scraper::getLatestPosts)
                .flatMap(Collection::stream)
                .filter(not(post -> posts.existsByExternalIdAndSource(post.getExternalId(), post.getSource())))
                .toList();

        unpublishedPosts.stream()
                .map(post -> save(post))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(post -> notifyUsers(post));
    }

    private boolean isNightTime() {
        var now = LocalDateTime.now();
        return now.getHour() >= 23 || now.getHour() < 6;
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
        logPost(post);
        if (post.getPrice().isEmpty()) {
            return;
        }

        getInterestedTelegramIds(post)
                .forEach(telegramId -> {
                    try {
                        bot.execute(postResponseCreator.createTelegramMessage(telegramId, post));
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
        if (post.getWithFees()) {
            return users.findAllTelegramIdsInterestedWithFee(price, rooms, year, floor);
        }
        return users.findAllTelegramIdsInterested(price, rooms, year, floor);
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
        if (post.getWithFees()) {
            return users.findAllTelegramIdsInterestedInDistrictWithFee(price, rooms, year, floor, district);
        }
        return users.findAllTelegramIdsInterestedInDistrict(price, rooms, year, floor, district);
    }

    private List<Long> findAllTelegramIdsNotInterestedInDistricts(Post post) {
        var price = post.getPrice().orElse(null);
        var rooms = post.getRooms().orElse(null);
        var year = post.getConstructionYear().orElse(null);
        var floor = post.getFloor().orElse(null);
        if (post.getWithFees()) {
            return users.findAllTelegramIdsNotInterestedInDistrictsWithFee(price, rooms, year, floor);
        }
        return users.findAllTelegramIdsNotInterestedInDistricts(price, rooms, year, floor);
    }

    private void logPost(Post post) {
        LOGGER.info("ID:{} Tel:{} Desc:{} Dist:{} Addr:{} Heat:{} Fl:{} FlTot:{} Area:{} Price:{} Room:{} Year:{} State:{} Material:{} WithFees:{} Link:{}",
                post.getId(), post.getPhone().orElse(null), post.getDescriptionHash().isPresent(),
                post.getDistrict().map(String::length).orElse(null), post.getStreet().map(String::length).orElse(null),
                post.getHeating().map(String::length).orElse(null), post.getFloor().orElse(null), post.getTotalFloors().orElse(null),
                post.getArea().map(area -> String.format("%.2f", area)).orElse(null), post.getPrice().map(price -> String.format("%.2f", price)).orElse(null),
                post.getRooms().orElse(null), post.getConstructionYear().orElse(null),
                post.getBuildingState().isPresent(), post.getBuildingMaterial().isPresent(),
                post.getWithFees(), post.getLink());
    }
}
