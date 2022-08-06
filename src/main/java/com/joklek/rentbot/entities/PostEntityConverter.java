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
        post.setPhone(getPhone(postDto));
        post.setDescriptionHash(getHash(postDto));
        post.setStreet(postDto.getStreet().trim());
        post.setDistrict(postDto.getDistrict().trim());
        post.setHouseNumber(postDto.getHouseNumber().trim());
        post.setHeating(postDto.getHeating().trim());
        post.setFloor(postDto.getFloor());
        post.setTotalFloors(postDto.getTotalFloors());
        post.setArea(postDto.getArea());
        post.setPrice(postDto.getPrice());
        post.setRooms(postDto.getRooms());
        post.setConstructionYear(postDto.getYear());

        post.setLastSeen(LocalDateTime.now());
        post.setWithFees(isWithFees(postDto));

        return post;
    }

    private String getPhone(PostDto postDto) {
        var phone = postDto.getPhone();
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

    private static final Map<String, String> LETTER_REPLACE_MAP = Map.of(
            "ą", "a",
            "č", "c",
            "ę", "e",
            "ė", "e",
            "į", "i",
            "š", "s",
            "ų", "u",
            "ū", "u",
            "ž", "z",
            "y", "i" // Replace y with i, because some people are bad at writing
    );

    private static final List<String> feeKeywords = List.of(
            "(yra mokestis)",
            "mokestis (jei butas",
            "\ntaikomas tarpininkavimas",
            "tiks vienkartinis tarpinink"
    );

    private static final List<Pattern> feePatterns = List.of(
            Pattern.compile("(agent|tarpinink|vienkart)\\S+ mokestis[\\s:-]{0,3}\\d+"),
            Pattern.compile("\\d+\\s?\\S+ (agent|tarpinink|vienkart)\\S+ (tarp|mokest)\\S+"),
            Pattern.compile("\\W(ira|bus) (taikoma(s|)|imama(s|)|vienkartinis|agent\\S+)( vienkartinis|) (agent|tarpinink|mokest)\\S+"),
            Pattern.compile("\\Wtiks[^\\s\\w]?\\s?(bus|ira|) (taikoma(s|)|imama(s|))"),
            Pattern.compile("\\W(ira |)(taikoma(s|)|imama(s|)|vienkartinis|sutarties)( sutarties|) sudar\\S+ mokestis"),
            Pattern.compile("(ui|ir) (ira |)(taikoma(s|)|imama(s|)) (vienkart|agent|tarpinink|mokest)\\S+"),
            Pattern.compile("(vienkartinis |)(agent|tarpinink)\\S+ mokest\\S+,? jei"),
            Pattern.compile("[^\\w\\s](\\s|)(taikoma(s|)|imama(s|)|vienkartinis|agent\\S+)( vienkartinis|) (agent|tarpinink|mokest)\\S+")
    );

    private boolean isWithFees(PostDto postDto) {
        var descriptionSimplified = postDto.getDescription();
        if (descriptionSimplified == null) {
            return false;
        }
        for (var entry : LETTER_REPLACE_MAP.entrySet()) {
            descriptionSimplified = descriptionSimplified.replace(entry.getKey(), entry.getValue());
        }

        for (var keyword : feeKeywords) {
            if (descriptionSimplified.contains(keyword)) {
                return true;
            }
        }

        for (var feePattern : feePatterns) {
            if (feePattern.matcher(descriptionSimplified).matches()) {
                return true;
            }
        }

        return false;
    }
}
