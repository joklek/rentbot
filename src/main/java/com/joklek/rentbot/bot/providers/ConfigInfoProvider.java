package com.joklek.rentbot.bot.providers;

import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ConfigInfoProvider {

    private final PostRepo posts;

    public ConfigInfoProvider(PostRepo posts) {
        this.posts = posts;
    }

    public String activeSettings(User user) {
        // TODO what when nothing configured?
        var showWithFees = user.getShowWithFees() ? "yes" : "no";
        var filterByDistrict = user.getFilterByDistrict() ? "yes" : "no";
        var weekBefore = LocalDateTime.now().minusDays(7);
        var listingsDuringLastWeek = posts.getCountOfPostsForUserFromDays(user.getId(), weekBefore);
        var statsText = user.isConfigured() ? String.format("You would've seen %d posts from last week with these settings\n", listingsDuringLastWeek) : "";
        return String.format(
                """
                %1$sShare your settings with other people by sharing this command:
                ```
                /config %2$.0f %3$.0f %4$d %5$d %6$d %7$d %8$s
                ```
                
                » *Filter by district:* %9$s (/districts to configure)
                """,
                statsText,
                user.getPriceMin().orElse(BigDecimal.ZERO), user.getPriceMax().orElse(BigDecimal.ZERO),
                user.getRoomsMin().orElse(0), user.getRoomsMax().orElse(0),
                user.getYearMin().orElse(0),
                user.getFloorMin().orElse(0),
                showWithFees,
                filterByDistrict);
    }
}
