package com.joklek.rentbot.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joklek.rentbot.repo.PostRepo;
import org.apache.hc.core5.net.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.openqa.selenium.InvalidArgumentException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.net.URI.create;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class OberHausScraper extends JsoupScraper {
    private static final Logger LOGGER = getLogger(OberHausScraper.class);
    private static final URI BASE_URL = URI.create("https://www.ober-haus.lt/api/object.php?sorting=newest&page=1&type=Apartment+for+sale&city=Vilniaus+m.+sav.&sorting_select=newest");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 14; Mobile; rv:129.0) Gecko/129.0 Firefox/129.0";
    private final PostRepo posts;

    private final HttpClient client;
    private final ObjectMapper mapper;

    public OberHausScraper(PostRepo posts, HttpClient client, ObjectMapper mapper) {
        this.posts = posts;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<PostDto> getLatestPosts(boolean fullScan) {
        var oldestPost = posts.findOldestBySource(OberHausPost.SOURCE);
        var partialPosts = new ArrayList<PostDto>();
        List<PostDto> lastPage = List.of();

        for (var page = 1; true; page++) {
            var rawPosts = getRawPostsInPage(page);
            var partialPostsFromPage = rawPosts.stream()
                    .map(rawPost -> {
                        try {
                            return processPartialItem(rawPost);
                        } catch (Exception e) {
                            LOGGER.error("Can't parse post '{}'", rawPost.get("id").textValue(), e);
                            return Optional.<PostDto>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            if (partialPostsFromPage.isEmpty()) {
                break;
            }
            if (page != 1 && Objects.equals(lastPage, partialPostsFromPage)) {
                LOGGER.info("Paging broken, reached page {}", page);
                break;
            }

            partialPosts.addAll(partialPostsFromPage);
            lastPage = partialPostsFromPage;
            if (page != 1) {
                LOGGER.info("Fetched {} posts from page {}", partialPostsFromPage.size(), page);
            }
            if (oldestPost.isEmpty()) {
                break; // Not sure if we should scrape all existing posts if we don't have any in the database
            }
            if (!fullScan) {
                break;
            }
            if (partialPostsFromPage.stream().map(PostDto::getExternalId)
                    .anyMatch(externalId -> oldestPost.get().getExternalId().compareTo(externalId) >= 0)) {
                break;
            }
        }

        return partialPosts.stream()
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

    private List<JsonNode> getRawPostsInPage(int pageNum) {
        var url = getLinkToPage(pageNum);
        var maybeTree = getPosts(url);
        if (maybeTree.isEmpty() || maybeTree.get().get("objects").isEmpty()) {
            return List.of();
        }
        return jsonNodeToList(maybeTree.get().get("objects"));
    }

    private static URI getLinkToPage(int page) {
        try {
            return new URIBuilder(BASE_URL).addParameter("page", Integer.toString(page)).build();
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to create link to page {}", page, e);
            throw new InvalidArgumentException("Failed to create link to page " + page, e);
        }
    }

    private Optional<PostDto> processPartialItem(JsonNode rawPost) {
        var rawHtml = rawPost.get("html").textValue();
        var document = Jsoup.parse(rawHtml);
        if (!document.select("span.reserved").isEmpty()) {
            return Optional.empty();
        }

        var id = rawPost.get("id").textValue();
        var link = create(String.format("https://www.ober-haus.lt/butas-pardavimas-vilniaus-m,%s", id));
        var price = Optional.ofNullable(document.select("div.price").first()).map(Element::text)
                .map(priceRaw -> priceRaw
                        .replace(" ", "")
                        .replace("€", "")
                        .trim()
                ).flatMap(ScraperHelper::parseBigDecimal);
        var post = new OberHausPost();

        post.setExternalId(id);
        post.setLink(link);
        price.ifPresent(post::setPrice);
        post.setPartial(true);

        return Optional.of(post);
    }

    private Optional<PostDto> processItem(PostDto post) {
        if (posts.existsByExternalIdAndSource(post.getExternalId(), OberHausPost.SOURCE)) {
            return Optional.of(post);
        }

        var maybeDocument = getDocument(post.getLink());
        if (maybeDocument.isEmpty()) {
            return Optional.empty();
        }
        var document = maybeDocument.get();

        var description = Optional.ofNullable(document.select("div.object__content_text").first()).map(Element::text).map(x -> x.replace("Apie šį turtą ", ""));
        var rawAddress = Optional.ofNullable(document.select("h1.title").first()).map(Element::text);
        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (rawAddress.isPresent() && rawAddress.get().contains(",")) {
            var splitAddress = rawAddress.get().split(", ");
            district = Optional.of(splitAddress[1]);
            if (splitAddress.length > 2) {
                street = Optional.of(splitAddress[2]);
            }
        }
        var price = Optional.ofNullable(document.select("span.price").first()).map(Element::text)
                .map(priceRaw -> priceRaw.replace(" ", "").trim())
                .flatMap(ScraperHelper::parseBigDecimal);

        var moreInfo = document.select("li.item").stream()
                .collect(Collectors.toMap(x -> x.removeClass("item").className(), x -> x.select("div.right").text()));
        var heating = Optional.ofNullable(moreInfo.get("item-heating"));
        var floors = Optional.ofNullable(moreInfo.get("item-floor"));
        var floor = floors.map(floorRaw -> floorRaw.split(" ")[0])
                .flatMap(ScraperHelper::parseInt);
        var totalFloors = floors.map(floorRaw -> floorRaw.split(" ")[2])
                .flatMap(ScraperHelper::parseInt);
        var area = Optional.ofNullable(moreInfo.get("item-area"))
                .map(areaRaw -> areaRaw.trim().split(" ")[0])
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("item-rooms"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("item-year"))
                .flatMap(ScraperHelper::parseInt);
        var shortLink = Optional.ofNullable(moreInfo.get("item-reference")).map(URI::create);

        shortLink.ifPresent(post::setLink);
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
        post.setPartial(false);

        return Optional.of(post);
    }

    private Optional<JsonNode> getPosts(URI link) {
        HttpResponse<String> response;
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(link)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US")
                .header("Upgrade-Insecure-Requests", "1")
                .build();
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            LOGGER.error("Failed while fetching '{}'", link, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            LOGGER.error("Failed while fetching '{}' with response code {}", link, response.statusCode());
            return Optional.empty();
        }

        try {
            return Optional.of(mapper.readTree(response.body()));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed while fetching '{}' because of invalid json", link, e);
            return Optional.empty();
        }
    }

    private List<JsonNode> jsonNodeToList(JsonNode node) {
        if (!node.isArray()) {
            LOGGER.warn("Expected array, but got {}", node.getNodeType());
            return List.of();
        }

        return StreamSupport.stream(node.spliterator(), false).toList();
    }


    private static class OberHausPost extends PostDto {
        private static final String SOURCE = "OberHaus";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
