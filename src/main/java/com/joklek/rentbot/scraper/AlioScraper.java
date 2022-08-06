package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AlioScraper implements Scraper {
    private static final Logger LOGGER = getLogger(AlioScraper.class);
    private static final URI BASE_URL = URI.create("https://www.alio.lt/paieska/?category_id=1393&city_id=228626&search_block=1&search[eq][adresas_1]=228626&order=ad_id");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 9; Mobile; rv:103.0) Gecko/103.0 Firefox/103.0";

    private final PostRepo posts;

    public AlioScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var maybeDoc = getDocument(BASE_URL);
        if (maybeDoc.isEmpty()) {
            // TODO log empty
            return List.of();
        }
        var doc = maybeDoc.get();

        var rawPosts = doc.select("#main_left_b > #main-content-center > div.result");

        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(Element rawPost) {
        var alioId = rawPost.attr("id").replace("lv_ad_id_", "");
        var link = URI.create(String.format("https://www.alio.lt/skelbimai/ID%s.html", alioId));
        if (posts.existsByExternalIdAndSource(alioId, AlioPost.SOURCE)) {
            return Optional.empty();
        }

        var maybeExactPost = getDocument(link);
        if (maybeExactPost.isEmpty()) {
            // TODO log empty
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var post = new AlioPost();
        var phone = Optional.ofNullable(exactPost.select("#phone_val_value").first()).map(Element::text);
        var description = Optional.ofNullable(exactPost.select("#adv_description_b > .a_line_val").first()).map(Element::text);

        var moreInfo = exactPost.select(".data_moreinfo_b").stream()
                .collect(Collectors.toMap(x -> x.select(".a_line_key").text(), x -> x.select(".a_line_val").text()));


        var rawAddress = Optional.ofNullable(moreInfo.get("Adresas"));
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (rawAddress.isPresent()) {
            var splitAddress = rawAddress.get().split(", ");
            district = Optional.of(splitAddress[1]);
            if (splitAddress.length > 2) {
                street = Optional.of(splitAddress[2]);
            }
        }
        var heating = Optional.ofNullable(moreInfo.get("Šildymas"));
        var floor = Optional.ofNullable(moreInfo.get("Buto aukštas"))
                .flatMap(ScraperHelper::parseInt);
        var totalFloors = Optional.ofNullable(moreInfo.get("Aukštų skaičius pastate"))
                .flatMap(ScraperHelper::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Buto plotas"))
                .map(areaRaw -> areaRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseBigDecimal);
        var price = Optional.ofNullable(moreInfo.get("Kaina, €"))
                .map(priceRaw -> priceRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambarių skaičius"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Statybos metai"))
                .map(yearRaw -> yearRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseInt);

        post.setExternalId(alioId);
        post.setLink(link);

        phone.ifPresent(post::setPhone);
        description.ifPresent(post::setDescription);
        district.ifPresent(post::setDistrict);
        street.ifPresent(post::setStreet);
        heating.ifPresent(post::setHeating);
        floor.ifPresent(post::setFloor);
        totalFloors.ifPresent(post::setTotalFloors);
        area.ifPresent(post::setArea);
        price.ifPresent(post::setPrice);
        rooms.ifPresent(post::setRooms);
        year.ifPresent(post::setYear);

        return Optional.of(post);
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

    private static class AlioPost extends PostDto {
        private static final String SOURCE = "ALIO";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
