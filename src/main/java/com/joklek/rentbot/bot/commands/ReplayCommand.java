package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.PostResponseCreator;
import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReplayCommand implements Command {

    private static final int POSTS_FROM_PREVIOUS_DAYS = 2;

    private final PostRepo posts;
    private final UserRepo users;
    private final PostResponseCreator postResponseCreator;

    public ReplayCommand(PostRepo posts, UserRepo users, PostResponseCreator postResponseCreator) {
        this.posts = posts;
        this.users = users;
        this.postResponseCreator = postResponseCreator;
    }

    @Override
    public String command() {
        return "/replay";
    }

    @Override
    public List<SendMessage> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if(!user.isConfigured()) {
            return simpleFinalResponse(update, "Please configure your settings with /config");
        }

        var posts = getPosts(user);
        var deduplicatedPosts = deduplicatePosts(posts);

        if (deduplicatedPosts.isEmpty()) {
            return simpleFinalResponse(update, String.format("No posts found in the last %d days", POSTS_FROM_PREVIOUS_DAYS));
        }

        var messages = new ArrayList<>(deduplicatedPosts.stream()
                .map(similarPosts -> postResponseCreator.createTelegramMessage(telegramId, similarPosts))
                .toList());
        messages.add(simpleResponse(update, String.format("Replayed %d posts from last %d days", posts.size(), POSTS_FROM_PREVIOUS_DAYS)));

        return messages;
    }

    private List<Post> getPosts(User user) {
        return posts.getAllPostsForUserFromDays(
                user.getId(),
                LocalDateTime.now().minusDays(POSTS_FROM_PREVIOUS_DAYS)
        );
    }

    private List<List<Post>> deduplicatePosts(List<Post> posts) {
        var deduplicatedPosts = new ArrayList<List<Post>>();
        for (var i = 0; i < posts.size() - 1; i++) {
            var post1 = posts.get(i);
            var postList = new ArrayList<Post>();
            postList.add(post1);
            for(var j = i + 1; j < posts.size(); j++) {
                var post2 = posts.get(j);

                if (post1.getSource().equals(post2.getSource())) {
                    continue;
                }
                if (post1.getPrice().equals(post2.getPrice())
                        && post1.getRooms().equals(post2.getRooms())
                        && post1.getConstructionYear().equals(post2.getConstructionYear())
                        && post1.getFloor().equals(post2.getFloor())
                        && post1.getTotalFloors().equals(post2.getTotalFloors())
                        && post1.getStreet().equals(post2.getStreet())
                ) {
                    postList.add(post2);
                }
            }

            deduplicatedPosts.add(postList);
        }

        return deduplicatedPosts;
    }
}
