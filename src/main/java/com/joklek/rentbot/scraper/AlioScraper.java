package com.joklek.rentbot.scraper;

import com.google.common.net.UrlEscapers;
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
public class AlioScraper extends JsoupScraper {
    private static final Logger LOGGER = getLogger(AlioScraper.class);
    private static final URI BASE_URL = URI.create("https://www.alio.lt/paieska/?category_id=1373&city_id=228626&search_block=1&search%5Beq%5D%5Badresas_1%5D=228626&order=ad_id");

    private final PostRepo posts;

    public AlioScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var maybeDoc = getDocument(BASE_URL);
        if (maybeDoc.isEmpty()) {
            return List.of();
        }
        var doc = maybeDoc.get();

        var rawPosts = doc.select("#main_left_b > #main-content-center > a.result");

        return rawPosts.stream()
                .map(rawPost -> {
                    try {
                        return processItem(rawPost);
                    } catch (Exception e) {
                        LOGGER.error("Can't parse post '{}'", rawPost.attr("href"), e);
                        return Optional.<PostDto>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(Element rawPost) {
        var rawLongLink = UrlEscapers.urlFragmentEscaper().escape(rawPost.attr("href"));
        var longLink = URI.create(rawLongLink);
        var linkElements = longLink.toString().split("/");
        var alioId = linkElements[linkElements.length - 1].replaceFirst(".html$", "").replaceFirst("^ID", "");
        var link = URI.create(String.format("https://www.alio.lt/skelbimai/ID%s.html", alioId));
        if (posts.existsByExternalIdAndSource(alioId, AlioPost.SOURCE)) {
            var partialPost = new AlioPost();
            partialPost.setPartial(true);
            partialPost.setExternalId(alioId);
            var price = Optional.ofNullable(rawPost.select(".main_price").first())
                    .map(Element::text)
                    .map(priceRaw -> priceRaw.trim()
                            .replace(" ", "")
                            .replace("€", ""))
                    .flatMap(ScraperHelper::parseBigDecimal);

            price.ifPresent(partialPost::setPrice);
            return Optional.of(partialPost);
        }

        var maybeExactPost = getDocument(longLink);
        if (maybeExactPost.isEmpty()) {
            // TODO log empty
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var post = new AlioPost();
        var description = Optional.ofNullable(exactPost.select("#adv_description_b > .a_line_val").first()).map(Element::text);

        var moreInfo = exactPost.select(".data_moreinfo_b").stream()
                .collect(Collectors.toMap(x -> x.select(".a_line_key").text(), x -> x.select(".a_line_val").text()));

        var rawAddress = Optional.ofNullable(moreInfo.get("Adresas"));
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (rawAddress.isPresent() && rawAddress.get().contains(",")) {
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
        var area = Optional.ofNullable(moreInfo.get("Būsto plotas"))
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
        var buildingState = Optional.ofNullable(moreInfo.get("Būsto būklė"));
        var buildingMaterial = Optional.ofNullable(moreInfo.get("Namo tipas"));

        post.setExternalId(alioId);
        post.setLink(link);

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
        buildingState.ifPresent(post::setBuildingState);
        buildingMaterial.ifPresent(post::setBuildingMaterial);

        return Optional.of(post);
    }

    private static class AlioPost extends PostDto {
        private static final String SOURCE = "ALIO";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
