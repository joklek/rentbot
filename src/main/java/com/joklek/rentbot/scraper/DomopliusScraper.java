package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DomopliusScraper extends JsoupScraper {
    private static final Logger LOGGER = getLogger(DomopliusScraper.class);
    private static final URI BASE_URL = URI.create("https://m.domoplius.lt/skelbimai/butai?action_type=1&address_1=461&category=1&order_by=1&order_direction=DESC");

    private final PostRepo posts;

    public DomopliusScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts(boolean fullScan) {
        var maybeDoc = getDocument(BASE_URL);
        if (maybeDoc.isEmpty()) {
            return List.of();
        }
        var doc = maybeDoc.get();

        var rawPosts = doc.select("ul.list > li[id^='ann_']");

        return rawPosts.stream()
                .map(rawPost -> {
                    try {
                        return processPartialItem(rawPost);
                    } catch (Exception e) {
                        LOGGER.error("Can't parse post '{}'", rawPost.attr("id").replace("ann_", ""), e);
                        return Optional.<PostDto>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(post -> {
                    try {
                        return processItem(post);
                    } catch (Exception e) {
                        LOGGER.error("Can't parse post '{}'", post.getLink(), e);
                        return Optional.<PostDto>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processPartialItem(Element rawPost) {
        var domoId = rawPost.attr("id").replace("ann_", "");
        var link = URI.create(rawPost.select("li a").attr("href"));
        var price = Optional.ofNullable(rawPost.select("span.price-list > strong").first())
                .map(Element::text)
                .map(priceRaw -> priceRaw.trim()
                        .replace(" ", "")
                        .replace("€", ""))
                .flatMap(ScraperHelper::parseBigDecimal);
        var post = new DomopliusPost();

        post.setPartial(true);
        post.setExternalId(domoId);
        post.setLink(link);
        price.ifPresent(post::setPrice);

        return Optional.of(post);
    }

    private Optional<PostDto> processItem(PostDto post) {
        if (posts.existsByExternalIdAndSource(post.getExternalId(), DomopliusPost.SOURCE)) {
            return Optional.of(post);
        }
        var maybeExactPost = getDocument(post.getLink()); // Not using created link because even with redirects turned on it doesn't work properly :/
        if (maybeExactPost.isEmpty()) {
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var description = Optional.ofNullable(exactPost.select("div.container > div.group-comments").first())
                .map(Element::text);

        var addressRaw = exactPost.select(".breadcrumb-item > a > span[itemprop=name]");
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (addressRaw.size() > 1) {
            district = Optional.of(addressRaw.get(1).text());
        }
        if (addressRaw.size() > 2) {
            street = Optional.of(addressRaw.get(2).text());
        }

        var price = Optional.ofNullable(exactPost.select(".field-price > .price-column > .h1").first())
                .map(Element::text)
                .map(priceRaw -> priceRaw.trim()
                        .replace(" ", "")
                        .replace("€", ""))
                .flatMap(ScraperHelper::parseBigDecimal);
        var moreInfo = exactPost.select(".view-field").stream()
                .filter(el -> !el.attr("title").isBlank())
                .collect(Collectors.toMap(x -> x.attr("title"), x -> x.textNodes().get(0).text().trim()));


        var houseNumber = Optional.ofNullable(moreInfo.get("Namo numeris"));
        var heating = Optional.ofNullable(moreInfo.get("Šildymas"));

        String rawFloor = null;
        String rawTotalFloors = null;
        var rawFloorInfo = moreInfo.get("Aukštas");
        if (rawFloorInfo != null) {
            if (rawFloorInfo.contains("aukštų pastate")) {
                var splitFloor = rawFloorInfo.replace("aukštų pastate", "").split(",");
                rawFloor = splitFloor[0];
                rawTotalFloors = splitFloor[1];
            } else {
                rawFloor = rawFloorInfo.split(" ")[0];
            }
        }
        var floor = Optional.ofNullable(rawFloor)
                .flatMap(ScraperHelper::parseInt);
        var totalFloors = Optional.ofNullable(rawTotalFloors)
                .flatMap(ScraperHelper::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Buto plotas (kv. m)"))
                .map(areaRaw -> areaRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambarių skaičius"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Statybos metai"))
                .map(yearRaw -> yearRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseInt);
        var buildingState = Optional.ofNullable(moreInfo.get("Būklė"));
        var buildingMaterial = Optional.ofNullable(moreInfo.get("Namo tipas"));
        var link = URI.create(String.format("https://domoplius.lt/skelbimai/-%s.html", post.getExternalId()));

        post.setLink(link);
        description.ifPresent(post::setDescription);
        district.ifPresent(post::setDistrict);
        street.ifPresent(post::setStreet);
        houseNumber.ifPresent(post::setHouseNumber);
        heating.ifPresent(post::setHeating);
        floor.ifPresent(post::setFloor);
        totalFloors.ifPresent(post::setTotalFloors);
        area.ifPresent(post::setArea);
        price.ifPresent(post::setPrice);
        rooms.ifPresent(post::setRooms);
        year.ifPresent(post::setYear);
        buildingState.ifPresent(post::setBuildingState);
        buildingMaterial.ifPresent(post::setBuildingMaterial);
        post.setPartial(false);

        return Optional.of(post);
    }

    private static class DomopliusPost extends PostDto {
        private static final String SOURCE = "DOMOPLIUS";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
