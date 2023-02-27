package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AruodasScraper implements Scraper {
    private static final Logger LOGGER = getLogger(AruodasScraper.class);
    private static final URI BASE_URL = URI.create("https://m.aruodas.lt/?obj=4&FRegion=461&FDistrict=1&FOrder=AddDate&from_search=1&detailed_search=1&FShowOnly=FOwnerDbId0%2CFOwnerDbId1&act=search");

    private final PostRepo posts;

    public AruodasScraper(PostRepo posts) {
        this.posts = posts;
    }

    @Override
    public List<PostDto> getLatestPosts() {
        var driver = new ChromeDriver();
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
        var rawPosts = driver.findElements(By.cssSelector("ul.search-result-list-big_thumbs > li:not([style='display: none'])"));
        if (rawPosts.isEmpty()) {
            LOGGER.error("Cant fetch posts, might be blocked");
            return List.of();
        }

        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost, driver))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(WebElement rawPost, WebDriver driver) {
        var originalWindow = driver.getWindowHandle();
        var aruodasId = rawPost.getAttribute("data-id").replace("loadobject", "");
        if (posts.existsByExternalIdAndSource(aruodasId, AruodasPost.SOURCE)) {
            return Optional.empty();
        }

        var link = URI.create(String.format("https://aruodas.lt/%s", aruodasId));
        driver.switchTo().newWindow(WindowType.TAB).get(link.toString());

        var post = new AruodasPost();
        var phone = selectByCss(driver, "span.phone_item_0")
                .orElseGet(() -> selectByCss(driver, "div.phone").orElse(null));
        var description = selectByCss(driver, "#collapsedTextBlock > #collapsedText");
        var rawAddress = selectByCss(driver, ".main-content > .obj-cont > h1").map(s -> List.of(s.split(",")).stream().filter(x -> !x.contains("buto nuoma")).toList());
        var district = rawAddress.flatMap(x -> x.size() >= 2 ? Optional.of(x.get(1)) : Optional.empty());
        var street = rawAddress.flatMap(x -> x.size() >= 3 ? Optional.of(x.get(2)) : Optional.empty());

        var objDetailsRaw = driver.findElements(By.cssSelector(".obj-details :not(hr)")).stream().toList();
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
        var price = Optional.ofNullable(moreInfo.get("Kaina mėn.:"))
                .map(x -> x.replace("€", ""))
                .map(x -> x.replace(" ", ""))
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambarių sk.:"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Metai:"))
                .map(x -> x.substring(0, 4))
                .flatMap(ScraperHelper::parseInt);

        post.setExternalId(aruodasId);
        post.setLink(link);
        post.setPhone(phone);
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
