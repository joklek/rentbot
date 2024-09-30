package com.joklek.rentbot.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.net.URI.create;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class KampasScraper implements Scraper {
    private static final URI BASE_URL = create("https://www.kampas.lt/api/classifieds/search-new?query=%7B%22municipality%22%3A%2258%22%2C%22settlement%22%3A19220%2C%22page%22%3A1%2C%22sort%22%3A%22new%22%2C%22section%22%3A%22bustas-nuomai%22%2C%22type%22%3A%22flat%22%7D");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 14; Mobile; rv:129.0) Gecko/129.0 Firefox/129.0";
    private static final Logger LOGGER = getLogger(KampasScraper.class);

    private final HttpClient client;

    private final ObjectMapper mapper;

    public KampasScraper(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var maybeTree = getPosts(BASE_URL);
        if (maybeTree.isEmpty() || maybeTree.get().get("hits").isEmpty() || !maybeTree.get().get("hits").isArray()) {
            // TODO log empty
            return List.of();
        }
        var rawPosts = StreamSupport.stream(maybeTree.get().get("hits").spliterator(), false).toList();

        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(JsonNode node) {
        var kampasId = node.get("id").asText();
        var link = create(String.format("https://www.kampas.lt/skelbimai/%s", kampasId));

        var post = new KampasPost();
        var description = Optional.ofNullable(node.get("description")).map(JsonNode::asText);
        var rawAddress = Optional.ofNullable(node.get("title")).map(JsonNode::asText);

        Optional<String> district = Optional.empty();
        Optional<String> street = Optional.empty();
        if (rawAddress.isPresent()) {
            var splitAddress = rawAddress.get().split(",");
            if (splitAddress.length >= 2) {
                district = Optional.of(splitAddress[1]);
            }
            if (splitAddress.length >= 3) {
                street = Optional.of(splitAddress[2]);
            }
        }

        Optional<String> heating = Optional.empty();
        var featureIterator = node.get("features").elements();
        while (node.get("features").isArray() && featureIterator.hasNext()) {
            var feature = featureIterator.next().asText();
            if (feature.endsWith("_heating")) {
                heating = Optional.of(feature
                        .replace("_heating", "")
                        .replace("gas", "dujinis")
                        .replace("central", "centrinis")
                        .replace("city", "centrinis")
                        .replace("thermostat", "termostatas"));
                break;
            }
        }
        var floor = Optional.ofNullable(node.get("objectfloor")).map(JsonNode::intValue).filter(x -> x > 0);
        var totalFloors = Optional.ofNullable(node.get("totalfloors")).map(JsonNode::intValue).filter(x -> x > 0);
        var area = Optional.ofNullable(node.get("objectarea"))
                .map(JsonNode::asText)
                .flatMap(ScraperHelper::parseBigDecimal);
        var price = Optional.ofNullable(node.get("objectprice"))
                .map(JsonNode::asText)
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(node.get("totalrooms")).map(JsonNode::intValue).filter(x -> x > 0);
        var year = Optional.ofNullable(node.get("yearbuilt")).map(JsonNode::intValue).filter(x -> x > 0);

        post.setExternalId(kampasId);
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

    private static class KampasPost extends PostDto {
        private static final String SOURCE = "KAMPAS";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
