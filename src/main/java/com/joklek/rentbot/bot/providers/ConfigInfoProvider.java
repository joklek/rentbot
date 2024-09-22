package com.joklek.rentbot.bot.providers;

import com.joklek.rentbot.bot.callbacks.ConfigCallback;
import com.joklek.rentbot.entities.User;
import com.joklek.rentbot.repo.PostRepo;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ConfigInfoProvider {

    private final PostRepo posts;
    private static final String SHARE_TEXT = """
                🔗 Share your settings with other people by sharing this command:
                ```
                /config %1$.0f %2$.0f %3$d %4$d %5$d %6$d %7$d %8$s
                ```
                """;

    public ConfigInfoProvider(PostRepo posts) {
        this.posts = posts;
    }

    public String activeSettings(User user) {
        // TODO what when nothing configured?
        var showWithFees = user.getShowWithFees() ? "yes" : "no";
        var filterByDistrict = user.getFilterByDistrict() ? "yes" : "no";
        var weekBefore = LocalDateTime.now().minusDays(7);
        var listingsDuringLastWeek = posts.getCountOfPostsForUserFromDays(user.getId(), weekBefore);
        var statsText = user.isConfigured() ? String.format("\uD83D\uDCCA You would've seen %d posts from last week with these settings.\n", listingsDuringLastWeek) : "";
        var shareText = user.isConfigured() ?
                String.format(SHARE_TEXT,
                        user.getPriceMin().orElse(BigDecimal.ZERO), user.getPriceMax().orElse(BigDecimal.ZERO),
                        user.getRoomsMin().orElse(0), user.getRoomsMax().orElse(0),
                        user.getAreaMin().orElse(0),
                        user.getYearMin().orElse(0),
                        user.getFloorMin().orElse(0),
                        showWithFees
                ) : "";

        return String.format(
                """
                %1$s
                %2$s
                🔄 *Filter by district*: %3$s (/districts to configure)
                """,
                statsText,
                shareText,
                filterByDistrict);
    }

    public InlineKeyboardMarkup showConfigPage(User user) {
        var keyboard = new InlineKeyboardMarkup();
        var enabledText = String.format("Notifications: %s", user.getEnabled() ? "Enabled" : "Disabled");
        var enabledButton = new InlineKeyboardButton(enabledText);
        enabledButton.callbackData(ConfigCallback.Toggle.CALLBACK_KEY);

        var priceMinText = String.format("Price from: %.0f€", user.getPriceMin().orElse(BigDecimal.ZERO));
        var priceMinButton = new InlineKeyboardButton(priceMinText);
        priceMinButton.callbackData(ConfigCallback.PriceMin.CALLBACK_KEY);

        var priceMaxText = String.format("Price to: %.0f€", user.getPriceMax().orElse(BigDecimal.ZERO));
        var priceMaxButton = new InlineKeyboardButton(priceMaxText);
        priceMaxButton.callbackData(ConfigCallback.PriceMax.CALLBACK_KEY);

        var roomsMinText = String.format("Min rooms: %d", user.getRoomsMin().orElse(0));
        var roomsMinButton = new InlineKeyboardButton(roomsMinText);
        roomsMinButton.callbackData(ConfigCallback.RoomsMin.CALLBACK_KEY);

        var roomsMaxText = String.format("Max rooms: %d", user.getRoomsMax().orElse(0));
        var roomsMaxButton = new InlineKeyboardButton(roomsMaxText);
        roomsMaxButton.callbackData(ConfigCallback.RoomsMax.CALLBACK_KEY);

        var constructionText = String.format("From construction year: %d", user.getYearMin().orElse(0));
        var constructionButton = new InlineKeyboardButton(constructionText);
        constructionButton.callbackData(ConfigCallback.ConstructionMin.CALLBACK_KEY);

        var floorText = String.format("Min floor: %d", user.getFloorMin().orElse(0));
        var floorButton = new InlineKeyboardButton(floorText);
        floorButton.callbackData(ConfigCallback.FloorMin.CALLBACK_KEY);

        var areaText = String.format("Min area m²: %d", user.getAreaMin().orElse(0));
        var areaButton = new InlineKeyboardButton(areaText);
        areaButton.callbackData(ConfigCallback.AreaMin.CALLBACK_KEY);

        var extraFeesText = String.format("Show with extra fees: %s", user.getShowWithFees() ? "Enabled" : "Disabled");
        var extraFeesButton = new InlineKeyboardButton(extraFeesText);
        extraFeesButton.callbackData(ConfigCallback.ToggleFees.CALLBACK_KEY);

        keyboard.addRow(enabledButton);
        keyboard.addRow(priceMinButton, priceMaxButton);
        keyboard.addRow(roomsMinButton, roomsMaxButton);
        keyboard.addRow(constructionButton);
        keyboard.addRow(floorButton, areaButton);
        keyboard.addRow(extraFeesButton);

        return keyboard;
    }
}
