package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DomopliusScraper implements Scraper {
    private static final Logger LOGGER = getLogger(DomopliusScraper.class);
    private static final URI BASE_URL = URI.create("https://m.domoplius.lt/skelbimai/butai?action_type=3&address_1=461&sell_price_from=&sell_price_to=&qt=");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 9; Mobile; rv:103.0) Gecko/103.0 Firefox/103.0";

    private final PostRepo posts;

    public DomopliusScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var maybeDoc = getDocument(BASE_URL);
        if (maybeDoc.isEmpty()) {
            return List.of();
        }
        var doc = maybeDoc.get();

        var rawPosts = doc.select("ul.list > li[id^='ann_']");

        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(Element rawPost) {
        var domoId = rawPost.attr("id").replace("ann_", "");
        var link = URI.create(String.format("https://domoplius.lt/skelbimai/-%s.html", domoId));
        if (posts.existsByExternalIdAndSource(domoId, DomopliusPost.SOURCE)) {
            return Optional.empty();
        }

        var maybeExactPost = getDocument(URI.create(rawPost.select("li a").attr("href"))); // Not using created link because even with redirects turned on it doesn't work properly :/
        if (maybeExactPost.isEmpty()) {
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var post = new DomopliusPost();
        var phone = Optional.ofNullable(exactPost.select("#phone_button_4 > span").first())
                .map(el -> el.attr("data-value"))
                .map(dataEncoded -> decode(dataEncoded));
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
                .map(priceRaw -> priceRaw.trim().split(" ")[0])
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

        post.setExternalId(domoId);
        post.setLink(link);

        phone.ifPresent(post::setPhone);
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

        return Optional.of(post);
    }

    private String decode(String dataEncoded) {
        return new String(Base64.getDecoder().decode(dataEncoded.substring(2)));
    }

    private Optional<Document> getDocument(URI link) {
        try {
            return Optional.of(Jsoup.connect(link.toString())
                    .header("Host", link.getHost())
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true)
                    .get());
        } catch (IOException e) {
            LOGGER.error("Failed while fetching '{}'", link, e);
            return Optional.empty();
        }
    }

    private static class DomopliusPost extends PostDto {
        private static final String SOURCE = "DOMOPLIUS";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
