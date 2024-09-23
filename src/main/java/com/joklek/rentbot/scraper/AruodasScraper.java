package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AruodasScraper implements Scraper {
    private static final Logger LOGGER = getLogger(AruodasScraper.class);
    private static final URI BASE_URL = URI.create("https://m.aruodas.lt/butai/vilniuje/?change_region=1&FOrder=AddDate");

    private final PostRepo posts;

    public AruodasScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var options = new FirefoxOptions().addArguments("-headless");
        var driver = new FirefoxDriver(options);
        try {
            return getPosts(driver);
        } catch (Exception e) {
            throw e; // TODO
        } finally {
            driver.quit();
        }
    }

    private List<PostDto> getPosts(WebDriver driver) {
        driver.get(BASE_URL.toString());
        var ids = driver.findElements(By.cssSelector("ul.search-result-list-big_thumbs > li.result-item-big-thumb:not([style='display: none'])"))
                .stream().flatMap(rawPost -> {
                    var relatedIds = rawPost.findElements(By.cssSelector("table tr td.goto > a")).stream().map(element -> element.getAttribute("href").split("/")[3]).toList();
                    var mainId = rawPost.getAttribute("data-id").replace("loadobject", "");
                    return Stream.concat(relatedIds.stream(), Stream.of(mainId));
                })
                .toList();
        if (ids.isEmpty()) {
            LOGGER.error("Cant fetch posts, might be blocked");
            return List.of();
        }

        return ids.stream()
                .map(aruodasId -> processItem(aruodasId, driver))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(String aruodasId, WebDriver driver) {
        var originalWindow = driver.getWindowHandle();

        if (posts.existsByExternalIdAndSource(aruodasId, AruodasPost.SOURCE)) {
            return Optional.empty();
        }

        var link = URI.create(String.format("https://aruodas.lt/%s", aruodasId));
        driver.switchTo().newWindow(WindowType.TAB).get(link.toString());

        var post = new AruodasPost();
        var description = selectByCss(driver, "#collapsedTextBlock > #collapsedText");
        var rawAddress = selectByCss(driver, ".main-content > .obj-cont > h1")
                .or(() -> selectByCss(driver, ".advert-info-header .title-col > h1"))
                .or(() -> selectByCss(driver, "div.obj-cont h1"))
                .map(s -> Stream.of(s.split(",")).toList());
        var district = rawAddress.flatMap(x -> x.size() >= 2 ? Optional.of(x.get(1)) : Optional.empty());
        var street = rawAddress.flatMap(x -> x.size() >= 3 ? Optional.of(x.get(2)) : Optional.empty());

        var price = selectByCss(driver, "span.price-eur")
                .map(rawPrice -> rawPrice.replace("€", "").replace(" ", "").trim())
                .flatMap(ScraperHelper::parseBigDecimal);

        var objDetailsRaw = driver.findElements(By.cssSelector(".obj-details > :not(hr)")).stream().toList();
        var moreInfo = objDetailsRaw.stream().collect(Collectors
                        .groupingBy(x -> objDetailsRaw.indexOf(x) / 2)).values().stream()
                .filter(x -> x.size() == 2)
                .map(x -> x.stream().map(WebElement::getText).toList())
                .filter(x -> !x.get(0).equals(x.get(1)))
                .filter(x -> !x.get(1).isBlank())
                .collect(Collectors.toMap(x -> x.get(0), x -> x.get(1)));
        var houseNumber = Optional.ofNullable(moreInfo.get("Namo numeris:"));
        var heating = Optional.ofNullable(moreInfo.get("Šildymas:"));
        var floor = Optional.ofNullable(moreInfo.get("Aukštas:"))
                .flatMap(ScraperHelper::parseInt);
        var totalFloors = Optional.ofNullable(moreInfo.get("Aukštų sk.:"))
                .flatMap(ScraperHelper::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Plotas:"))
                .map(x -> x.replace(" m²", ""))
                .map(x -> x.replace(",", "."))
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambarių sk.:"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Metai:"))
                .map(x -> x.substring(0, 4))
                .flatMap(ScraperHelper::parseInt);
        var buildingState = Optional.ofNullable(moreInfo.get("Įrengimas:"));
        var buildingMaterial = Optional.ofNullable(moreInfo.get("Pastato tipas:"));

        post.setExternalId(aruodasId);
        post.setLink(link);
        district.ifPresent(post::setDistrict);
        street.ifPresent(post::setStreet);
        houseNumber.ifPresent(post::setHouseNumber);
        description.ifPresent(post::setDescription);
        heating.ifPresent(post::setHeating);
        floor.ifPresent(post::setFloor);
        totalFloors.ifPresent(post::setTotalFloors);
        area.ifPresent(post::setArea);
        price.ifPresent(post::setPrice);
        rooms.ifPresent(post::setRooms);
        year.ifPresent(post::setYear);
        buildingState.ifPresent(post::setBuildingState);
        buildingMaterial.ifPresent(post::setBuildingMaterial);

        driver.close();
        driver.switchTo().window(originalWindow);

        return Optional.of(post);
    }

    private Optional<String> selectByCss(WebDriver driver, String cssSelector) {
        var elements = driver.findElements(By.cssSelector(cssSelector));
        if (elements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(elements.get(0).getText());
    }

    private static class AruodasPost extends PostDto {
        private static final String SOURCE = "ARUODAS";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
