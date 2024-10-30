package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class CapitalScraper extends JsoupScraper {
    private static final URI BASE_URL = URI.create("https://www.capital.lt/lt/nekilnojamas-turtas/butai-pardavimui/vilniaus-m-sav/vilnius");
    private static final Logger LOGGER = getLogger(CapitalScraper.class);

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

        var rawPosts = doc.select("div.realty-items > a:not(.realty-status-sold)");

        return rawPosts.stream()
                .map(rawPost -> {
                    try {
                        return processPartialItem(rawPost);
                    } catch (Exception e) {
                        LOGGER.error("Can't parse post '{}'", rawPost.attr("id").replace("item-", ""), e);
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
        var capitalId = rawPost.attr("id").replace("item-", "");
        var link = URI.create(String.format("https://www.capital.lt/lt/p%s", capitalId));
        var price = Optional.ofNullable(rawPost.select("div.realty-item-price > strong").first())
                .map(Element::text)
                .map(priceRaw -> priceRaw.trim()
                        .replace(" ", "")
                        .replace(",", "")
                        .replace("€", ""))
                .flatMap(ScraperHelper::parseBigDecimal);
        var post = new CapitalPost();

        post.setExternalId(capitalId);
        post.setLink(link);
        price.ifPresent(post::setPrice);
        post.setPartial(true);

        return Optional.of(post);
    }

    private Optional<PostDto> processItem(PostDto post) {
        if (posts.existsByExternalIdAndSource(post.getExternalId(), CapitalPost.SOURCE)) {
            return Optional.of(post);
        }
        var maybeExactPost = getDocument(post.getLink());
        if (maybeExactPost.isEmpty()) {
            return Optional.empty();
        }
        var exactPost = maybeExactPost.get();
        var description = Optional.ofNullable(exactPost.select(".realty-description").first())
                .map(Element::text);

        var price = Optional.ofNullable(exactPost.select(".realty-price-info > strong").first())
                .map(Element::text)
                .map(priceRaw -> priceRaw.trim()
                        .replace("Kaina:", "")
                        .replace(" ", "")
                        .replace(",", "")
                        .replace("€", ""))
                .flatMap(ScraperHelper::parseBigDecimal);

        var moreInfo = exactPost.select(".realty-info-line").stream()
                .filter(el -> el.select("td").size() == 2)
                .collect(Collectors.toMap(
                        x -> x.select("td").get(0).text(),
                        x -> {
                            var node = x.select("td").get(1);
                            return (node.select("div").isEmpty() ? node : node.select("div").get(0)).text().trim();
                        }));

        var addresRaw = Optional.ofNullable(moreInfo.get("Adresas"));
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        Optional<String> houseNumber = Optional.empty();
        if (addresRaw.isPresent()) {
            var addressSplit = addresRaw.get().split(",");
            if (addressSplit.length == 4) {
                var realStreet = addressSplit[0];
                street = Optional.of(realStreet);
                district = Optional.of(addressSplit[1]);
                var postTitle = exactPost.select("h1.realty-title").text();
                var matcher = Pattern.compile(String.format(".+%s (\\w+),.+", realStreet)).matcher(postTitle);
                if (matcher.matches()) {
                    houseNumber = Optional.of(matcher.group(1));
                }
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
        var buildingState = Optional.ofNullable(moreInfo.get("Įrengimo lygis"));
        var buildingMaterial = Optional.ofNullable(moreInfo.get("Statinio tipas"));

        description.ifPresent(post::setDescription);
        district.ifPresent(post::setDistrict);
        street.ifPresent(post::setStreet);
        heating.ifPresent(post::setHeating);
        floor.ifPresent(post::setFloor);
        totalFloors.ifPresent(post::setTotalFloors);
        houseNumber.ifPresent(post::setHouseNumber);
        area.ifPresent(post::setArea);
        price.ifPresent(post::setPrice);
        rooms.ifPresent(post::setRooms);
        year.ifPresent(post::setYear);
        buildingState.ifPresent(post::setBuildingState);
        buildingMaterial.ifPresent(post::setBuildingMaterial);
        post.setPartial(false);

        return Optional.of(post);
    }

    private static class CapitalPost extends PostDto {
        private static final String SOURCE = "CAPITAL";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
