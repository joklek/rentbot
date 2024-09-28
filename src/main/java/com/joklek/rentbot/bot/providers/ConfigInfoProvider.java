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
                /config %1$s %2$s %3$s %4$s %5$s %6$s %7$s
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
                        user.getPriceMin().map(x -> String.format("%.0f", x)).orElse("any"),
                        user.getPriceMax().map(x -> String.format("%.0f", x)).orElse("any"),
                        user.getRoomsMin().map(Object::toString).orElse("any"),
                        user.getRoomsMax().map(Object::toString).orElse("any"),
                        user.getYearMin().map(Object::toString).orElse("any"),
                        user.getFloorMin().map(Object::toString).orElse("any"),
                        showWithFees
                ) : "";
        var notificationReminder = user.isConfigured() ? (user.getEnabled() ? "\n🔔 Scanning is enabled" : "\n🔕 Scanning is **disabled**. You will not see new posts") : "";

        return String.format(
                """
                %1$s
                %2$s
                🔄 *Filter by district*: %3$s (/districts to configure)
                %4$s
                """,
                statsText,
                shareText,
                filterByDistrict,
                notificationReminder);
    }

    public InlineKeyboardMarkup showConfigPage(User user) {
        var keyboard = new InlineKeyboardMarkup();
        var enabledText = user.getEnabled() ? "Disable scanning" : "Click to start scanning for listings!";
        var enabledButton = new InlineKeyboardButton(enabledText);
        enabledButton.callbackData(ConfigCallback.Toggle.CALLBACK_KEY);

        var priceMinText = String.format("From: %s€", user.getPriceMin().map(x -> String.format("%.0f", x)).orElse("any"));
        var priceMinButton = new InlineKeyboardButton(priceMinText);
        priceMinButton.callbackData(ConfigCallback.PriceMin.CALLBACK_KEY);

        var priceMaxText = String.format("To: %s€", user.getPriceMax().map(x -> String.format("%.0f", x)).orElse("any"));
        var priceMaxButton = new InlineKeyboardButton(priceMaxText);
        priceMaxButton.callbackData(ConfigCallback.PriceMax.CALLBACK_KEY);

        var roomsMinText = String.format("Min rooms: %s", user.getRoomsMin().map(Object::toString).orElse("any"));
        var roomsMinButton = new InlineKeyboardButton(roomsMinText);
        roomsMinButton.callbackData(ConfigCallback.RoomsMin.CALLBACK_KEY);

        var roomsMaxText = String.format("Max rooms: %s", user.getRoomsMax().map(Object::toString).orElse("any"));
        var roomsMaxButton = new InlineKeyboardButton(roomsMaxText);
        roomsMaxButton.callbackData(ConfigCallback.RoomsMax.CALLBACK_KEY);

        var constructionText = String.format("From construction year: %s", user.getYearMin().map(Object::toString).orElse("any"));
        var constructionButton = new InlineKeyboardButton(constructionText);
        constructionButton.callbackData(ConfigCallback.ConstructionMin.CALLBACK_KEY);

        var floorText = String.format("Min floor: %s", user.getFloorMin().map(Object::toString).orElse("any"));
        var floorButton = new InlineKeyboardButton(floorText);
        floorButton.callbackData(ConfigCallback.FloorMin.CALLBACK_KEY);

        var extraFeesText = String.format("Show with extra fees: %s", user.getShowWithFees() ? "Enabled" : "Disabled");
        var extraFeesButton = new InlineKeyboardButton(extraFeesText);
        extraFeesButton.callbackData(ConfigCallback.ToggleFees.CALLBACK_KEY);

        keyboard.addRow(enabledButton);
        keyboard.addRow(priceMinButton, priceMaxButton);
        keyboard.addRow(roomsMinButton, roomsMaxButton);
        keyboard.addRow(constructionButton);
        keyboard.addRow(floorButton);
        keyboard.addRow(extraFeesButton);

        return keyboard;
    }
}
