package com.joklek.rentbot.entities;

import com.joklek.rentbot.scraper.PostDto;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
public class PostEntityConverter {

    public Post convert(PostDto postDto) {
        var post = new Post();
        post.setSource(postDto.getSource());
        post.setExternalId(postDto.getExternalId());
        post.setLink(postDto.getLink().toString());
        post.setPhone(postDto.getPhone());
        post.setDescriptionHash(getHash(postDto));
        post.setStreet(postDto.getStreet());
        post.setDistrict(postDto.getDistrict());
        post.setHouseNumber(postDto.getHouseNumber());
        post.setHeating(postDto.getHeating());
        post.setFloor(postDto.getFloor());
        post.setTotalFloors(postDto.getTotalFloors());
        post.setArea(postDto.getArea());
        post.setPrice(postDto.getPrice());
        post.setRooms(postDto.getRooms());
        post.setConstructionYear(postDto.getYear());

        post.setLastSeen(LocalDateTime.now());
        post.setWithFees(false);

        return post;
    }

    private String getHash(PostDto postDto) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(postDto.getDescription().getBytes());
        return Base64.getEncoder().encodeToString(messageDigest.digest());
    }
}
