package com.joklek.rentbot.scraper;

import java.util.List;

public interface Scraper {
    List<PostDto> getLatestPosts();
}
