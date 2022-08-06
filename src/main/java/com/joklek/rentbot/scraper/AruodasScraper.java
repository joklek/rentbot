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
public class AruodasScraper implements Scraper {
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
        var rawPosts = driver.findElements(By.cssSelector("ul.search-result-list-v2 > li.result-item-v3:not([style='display: none'])"));
        return rawPosts.stream()
                .map(rawPost -> processItem(rawPost, driver))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PostDto> processItem(WebElement rawPost, WebDriver driver) {
        var originalWindow = driver.getWindowHandle();
        var aruodasId = rawPost.getAttribute("data-id").replace("loadObject", "");
        if (posts.existsByExternalIdAndSource(aruodasId, AruodasPost.SOURCE)) {
            return Optional.empty();
        }

        var link = URI.create(String.format("https://aruodas.lt/%s", aruodasId));
        driver.switchTo().newWindow(WindowType.TAB).get(link.toString());

        var post = new AruodasPost();
        var phone = selectByCss(driver, "span.phone_item_0")
                .orElseGet(() -> selectByCss(driver, "div.phone").orElse(null));
        var description = selectByCss(driver, "#collapsedTextBlock > #collapsedText");
        var rawAddress = selectByCss(driver, ".main-content > .obj-cont > h1").map(s -> s.split(","));

        // TODO move out int parsing and stuff out to EntityConvertor to make this cleaner
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
                .flatMap(this::parseInt);
        var totalFloors = Optional.ofNullable(moreInfo.get("Aukštų sk.:"))
                .flatMap(this::parseInt);
        var area = Optional.ofNullable(moreInfo.get("Plotas:"))
                .map(x -> x.replace(" m²", ""))
                .flatMap(this::parseBigDecimal);
        var price = Optional.ofNullable(moreInfo.get("Kaina mėn.:"))
                .map(x -> x.replace("€", ""))
                .map(x -> x.replace(" ", ""))
                .flatMap(this::parseBigDecimal);
        var rooms = Optional.ofNullable(moreInfo.get("Kambarių sk.:"))
                .flatMap(this::parseInt);
        var year = Optional.ofNullable(moreInfo.get("Metai:"))
                .map(x -> x.substring(0, 4))
                .flatMap(this::parseInt);

        post.setExternalId(aruodasId)
                .setLink(link)
                .setPhone(phone);
        rawAddress.ifPresent(splitAddress -> post.setDistrict(splitAddress[1]));
        rawAddress.ifPresent(splitAddress -> post.setStreet(splitAddress[2]));
        description.ifPresent(post::setDescription);
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

    private static class AruodasPost extends PostDto {
        private static final String SOURCE = "ARUODAS";

        @Override
        public String getSource() {
            return SOURCE;
        }
    }
}
