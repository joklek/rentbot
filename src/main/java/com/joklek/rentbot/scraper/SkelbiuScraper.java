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

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class SkelbiuScraper implements Scraper {
    private static final Logger LOGGER = getLogger(SkelbiuScraper.class);
    private static final URI BASE_URL = URI.create("https://www.skelbiu.lt/skelbimai/?cities=465&category_id=322&cities=465&district=0&cost_min=&cost_max=&status=0&space_min=&space_max=&rooms_min=&rooms_max=&building=0&year_min=&year_max=&floor_min=&floor_max=&floor_type=0&user_type=0&type=1&orderBy=1&import=2&keywords=");

    private final PostRepo posts;

    public SkelbiuScraper(PostRepo posts) {
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
        var rawPosts = driver.findElements(By.cssSelector("div.standard-list-container > div > a.standard-list-item[data-item-id]"));
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
        var skelbiuId = rawPost.getAttribute("data-item-id");
        if (posts.existsByExternalIdAndSource(skelbiuId, SkelbiuPost.SOURCE)) {
            return Optional.empty();
        }

        var link = URI.create(String.format("https://skelbiu.lt/skelbimai/%s.html", skelbiuId));
        driver.switchTo().newWindow(WindowType.TAB).get(link.toString());

        var post = new SkelbiuPost();
        var description = selectByCss(driver, "div[itemprop='description']");
        var price = selectByCss(driver, "p.price")
                .map(x -> x.replace("€", ""))
                .map(x -> x.replace(" ", ""))
                .flatMap(ScraperHelper::parseBigDecimal);

        var thisPage = driver.findElement(By.cssSelector("html"));
        var moreInfo = thisPage.findElements(By.cssSelector(".details-row:not(.features-list)")).stream()
                .collect(Collectors.toMap(x -> By.tagName("label").findElement(x).getText(),
                        x -> By.tagName("span").findElement(x).getText()));

        var district = Optional.ofNullable(moreInfo.get("Mikrorajonas:"));
        var street = Optional.ofNullable(moreInfo.get("Gatvė:"));
        var houseNumber = Optional.ofNullable(moreInfo.get("Namo numeris:"));
        var heating = Optional.ofNullable(moreInfo.get("Šildymas:"));
        var floor = Optional.ofNullable(moreInfo.get("Aukštas:"))
                .flatMap(ScraperHelper::parseInt);
        var totalFloors = Optional.ofNullable(moreInfo.get("Aukštų skaičius:"))
                .flatMap(ScraperHelper::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Plotas, m²:"))
                .map(areaRaw -> areaRaw.replace(" m²", ""))
                .map(areaRaw -> areaRaw.replace(",", "."))
                .flatMap(ScraperHelper::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kamb. sk.:"))
                .flatMap(ScraperHelper::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Metai:"))
                .flatMap(ScraperHelper::parseInt);

        post.setExternalId(skelbiuId);
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

    private static class SkelbiuPost extends PostDto {
        private static final String SOURCE = "SKELBIU";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
