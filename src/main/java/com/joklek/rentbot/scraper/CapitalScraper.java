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
public class CapitalScraper implements Scraper {
    private static final Logger LOGGER = getLogger(CapitalScraper.class);
    private static final URI BASE_URL = URI.create("https://www.capital.lt/lt/nekilnojamas-turtas/butai-nuomai/vilniaus-m-sav/vilnius");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 9; Mobile; rv:103.0) Gecko/103.0 Firefox/103.0";

    private final PostRepo posts;

    public CapitalScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var maybeDoc = getDocument(BASE_URL);
        if (maybeDoc.isEmpty()) {
            return List.of();
        }
        var doc = maybeDoc.get();

        var rawPosts = doc.select("div.realty-items > a");

        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(Element rawPost) {
        var capitalId = rawPost.attr("id").replace("item-", "");
        var link = URI.create(rawPost.attr("href"));
        if (posts.existsByExternalIdAndSource(capitalId, CapitalPost.SOURCE)) {
            return Optional.empty();
        }

        var maybeExactPost = getDocument(link);
        if (maybeExactPost.isEmpty()) {
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var post = new CapitalPost();
        var phone = Optional.ofNullable(exactPost.select(".estate-phone-sticker a").first())
                .map(Element::text);
        var description = Optional.ofNullable(exactPost.select(".realty-description").first())
                .map(Element::text);

        var price = Optional.ofNullable(exactPost.select(".realty-price-info > strong").first())
                .map(Element::text)
                .map(priceRaw -> priceRaw.trim()
                        .replace("Nuoma:", "")
                        .replace(" ", "")
                        .replace(",", "")
                        .replace("€", ""))
                .flatMap(ScraperHelper::parseBigDecimal);

        var moreInfo = exactPost.select(".realty-info-line").stream()
                .filter(el -> el.select("td").size() == 2)
                .collect(Collectors.toMap(x -> x.select("td").get(0).text(), x -> x.select("td").get(1).text().trim()));

        var addresRaw = Optional.ofNullable(moreInfo.get("Adresas"));
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (addresRaw.isPresent()) {
            var addressSplit = addresRaw.get().split(",");
            if (addressSplit.length == 4) {
                street = Optional.of(addressSplit[0]);
                district = Optional.of(addressSplit[1]);
            }
        }

        var heating = Optional.ofNullable(moreInfo.get("Šildymas"));
        Optional<Integer> floor = Optional.empty();
        Optional<Integer> totalFloors = Optional.empty();
        var floorRaw = Optional.ofNullable(moreInfo.get("Aukštas"));
        if (floorRaw.isPresent()) {
            var floorSplit = floorRaw.get().split("/");
            floor = ScraperHelper.parseInt(floorSplit[0]);
            if (floorSplit.length == 2) {
                totalFloors = ScraperHelper.parseInt(floorSplit[1]);
            }
        }
        var area = Optional.ofNullable(moreInfo.get("Plotas"))
                .map(areaRaw -> areaRaw.split(" ")[0])
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambariai"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Statybos metai"))
                .map(yearRaw -> yearRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseInt);

        post.setExternalId(capitalId);
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

    private static class CapitalPost extends PostDto {
        private static final String SOURCE = "CAPITAL";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
