package com.joklek.rentbot.bot.providers;

import com.joklek.rentbot.entities.Post;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostDeduplicator {
    public List<List<Post>> deduplicatePosts(List<Post> posts) {
        var deduplicatedPosts = new ArrayList<List<Post>>();
        for (var i = 0; i < posts.size(); i++) {
            var post1 = posts.get(i);

            var duplicateListsContainsPost = false;
            for (var j = 0; j < i; j++) {
                duplicateListsContainsPost = deduplicatedPosts.stream().anyMatch(deduplicated -> deduplicated.contains(post1));
                if (duplicateListsContainsPost) {
                    break;
                }
            }
            if (duplicateListsContainsPost) {
                continue;
            }
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
                } else if (post1.getPrice().equals(post2.getPrice()) && post1.getDescriptionHash().equals(post2.getDescriptionHash())) {
                    postList.add(post2);
                }
            }

            deduplicatedPosts.add(postList);
        }

        return deduplicatedPosts;
    }
}
