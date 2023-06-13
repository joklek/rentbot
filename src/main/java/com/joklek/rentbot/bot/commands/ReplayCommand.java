package com.joklek.rentbot.bot.commands;

import com.joklek.rentbot.bot.PostResponseCreator;
import com.joklek.rentbot.entities.Post;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import com.joklek.rentbot.repo.UserRepo;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
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
    public List<BaseRequest<?, ?>> handle(Update update, String payload) {
        var telegramId = update.message().chat().id();
        var user = users.getByTelegramId(telegramId);
        if(!user.isConfigured()) {
            return simpleFinalResponse(update, "Please configure your settings with /config");
        }

        var posts = getPosts(user);

        if (posts.isEmpty()) {
            return simpleFinalResponse(update, String.format("No posts found in the last %d days", POSTS_FROM_PREVIOUS_DAYS));
        }

        var messages = new ArrayList<BaseRequest<?, ?>>(posts.stream()
                .map(post -> postResponseCreator.createTelegramMessage(telegramId, post))
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
}
