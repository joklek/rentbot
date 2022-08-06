package com.joklek.rentbot.scraper;

import com.joklek.rentbot.repo.PostRepo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SkelbiuScraper implements Scraper {
    private static final URI BASE_URL = URI.create("https://www.skelbiu.lt/skelbimai/?cities=465&category_id=322&cities=465&district=0&cost_min=&cost_max=&status=0&space_min=&space_max=&rooms_min=&rooms_max=&building=0&year_min=&year_max=&floor_min=&floor_max=&floor_type=0&user_type=0&type=1&orderBy=1&import=2&keywords=");

    private final PostRepo posts;

    public SkelbiuScraper(PostRepo posts) {
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
        var rawPosts = driver.findElements(By.cssSelector("#itemsList > ul > li.simpleAds:not(.passivatedItem)"));
        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost, driver))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(WebElement rawPost, WebDriver driver) {
        var originalWindow = driver.getWindowHandle();
        var skelbiuId = rawPost.findElement(By.cssSelector("a.adsImage[data-item-id]")).getAttribute("data-item-id");
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
                .flatMap(this::parseBigDecimal);

        var thisPage = driver.findElement(By.cssSelector("html"));
        var moreInfo = thisPage.findElements(By.className("detail")).stream()
                .collect(Collectors.toMap(x -> By.className("title").findElement(x).getText(), x -> By.className("value").findElement(x).getText()));

        var district = Optional.ofNullable(moreInfo.get("Mikrorajonas:"));
        var street = Optional.ofNullable(moreInfo.get("Gatvė:"));
        var houseNumber = Optional.ofNullable(moreInfo.get("Namo numeris:"));
        var heating = Optional.ofNullable(moreInfo.get("Šildymas:"));
        var floor = Optional.ofNullable(moreInfo.get("Aukštas:"))
                .flatMap(this::parseInt);
        var totalFloors = Optional.ofNullable(moreInfo.get("Aukštų skaičius:"))
                .flatMap(this::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Plotas, m²:"))
                .map(areaRaw -> areaRaw.replace(" m²", ""))
                .map(areaRaw -> areaRaw.replace(",", "."))
                .flatMap(this::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kamb. sk.:"))
                .flatMap(this::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Metai:"))
                .flatMap(this::parseInt);

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

    private Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            // TODO log bad parse
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parseBigDecimal(String s) {
        try {
            return Optional.of(new BigDecimal(s.trim()));
        } catch (Exception e) {
            // TODO log bad parse
            return Optional.empty();
        }
    }

    private static class SkelbiuPost extends PostDto {
        private static final String SOURCE = "SKELBIU";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
