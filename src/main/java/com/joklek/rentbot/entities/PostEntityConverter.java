package com.joklek.rentbot.entities;

import com.joklek.rentbot.scraper.PostDto;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PostEntityConverter {
    private static final Logger LOGGER = getLogger(PostEntityConverter.class);

    public Post convert(PostDto postDto) {
        var post = new Post();
        post.setSource(postDto.getSource());
        post.setExternalId(postDto.getExternalId());
        post.setLink(postDto.getLink().toString());
        postDto.getDescription()
                .filter(x -> !x.isBlank())
                .map(x -> x.replace("<br>", "\n")
                        .replace("</br>", "\n"))
                .map(this::getHash)
                .ifPresent(post::setDescriptionHash);
        postDto.getStreet()
                .map(street -> street.trim())
                .ifPresent(post::setStreet);
        postDto.getDistrict()
                .map(street -> street.trim())
                .ifPresent(post::setDistrict);
        postDto.getHouseNumber()
                .map(street -> street.trim())
                .ifPresent(post::setHouseNumber);
        postDto.getHeating()
                .map(street -> street.trim())
                .ifPresent(post::setHeating);
        postDto.getFloor()
                .ifPresent(post::setFloor);
        postDto.getTotalFloors()
                .ifPresent(post::setTotalFloors);
        postDto.getArea()
                .ifPresent(post::setArea);
        postDto.getPrice()
                .ifPresent(price -> {
                    post.setPrice(price);
                    post.addPostPriceHistory(new PostPriceHistory(price));
                });
        postDto.getRooms()
                .ifPresent(post::setRooms);
        postDto.getYear()
                .ifPresent(post::setConstructionYear);
        post.setBuildingMaterial(postDto.getBuildingMaterial().orElse(null));
        post.setBuildingState(postDto.getBuildingState().orElse(null));

        if (postDto.getStreet().isPresent() && postDto.getHouseNumber().isEmpty() && postDto.getDescription().isPresent()) {
            var description = postDto.getDescription().get();
            var street = postDto.getStreet().get();
            var streetPattern = Pattern.compile(String.format("%s (\\d+[A-Z])", street), Pattern.CASE_INSENSITIVE);
            var matcher = streetPattern.matcher(description);
            if (matcher.find()) {
                LOGGER.info("Found house number in description: {} of {} {}", matcher.group(1), postDto.getSource(), postDto.getExternalId());
                post.setHouseNumber(matcher.group(1));
            }
        }

        post.setCreatedAt(LocalDateTime.now());

        return post;
    }

    private String getHash(String description) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(description.getBytes());
        return Base64.getEncoder().encodeToString(messageDigest.digest());
    }
}
