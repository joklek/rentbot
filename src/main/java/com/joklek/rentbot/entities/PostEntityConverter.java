package com.joklek.rentbot.entities;

import com.joklek.rentbot.scraper.PostDto;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class PostEntityConverter {

    public Post convert(PostDto postDto) {
        var post = new Post();
        post.setSource(postDto.getSource());
        post.setExternalId(postDto.getExternalId());
        post.setLink(postDto.getLink().toString());
        postDto.getPhone()
                .map(this::getPhone)
                .ifPresent(post::setPhone);
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
                .ifPresent(post::setPrice);
        postDto.getRooms()
                .ifPresent(post::setRooms);
        postDto.getYear()
                .ifPresent(post::setConstructionYear);
        post.setBuildingMaterial(postDto.getBuildingMaterial().orElse(null));
        post.setBuildingState(postDto.getBuildingState().orElse(null));

        post.setCreatedAt(LocalDateTime.now());

        return post;
    }

    private String getPhone(String phone) {
        phone = phone.replace(" ", "");
        if (phone.startsWith("00")) {
            phone = phone.replaceFirst("00", "");
        }
        if (phone.startsWith("370")) {
            phone = "+" + phone;
        } else if (phone.startsWith("86")) {
            phone = phone.replaceFirst("86", "+3706");
        }
        return phone.trim();
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
