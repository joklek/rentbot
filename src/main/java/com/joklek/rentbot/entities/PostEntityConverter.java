package com.joklek.rentbot.entities;

import com.joklek.rentbot.scraper.PostDto;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
        postDto.getPhone()
                .map(this::getPhone)
                .ifPresent(post::setPhone);
        postDto.getDescription()
                .filter(x -> !x.isBlank())
                .map(x -> x.replaceAll("\\s", "").trim())
                .map(this::getHash)
                .ifPresent(post::setDescriptionHash);
        postDto.getStreet()
                .map(street -> street.trim())
                .ifPresent(post::setStreet);
        postDto.getDistrict()
                .map(district -> district.trim())
                .ifPresent(post::setDistrict);
        postDto.getHouseNumber()
                .map(number -> number.trim().toUpperCase())
                .ifPresent(post::setHouseNumber);
        postDto.getHeating()
                .map(heating -> heating.trim())
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
        post.setWithFees(postDto.getDescription()
                .map(this::isWithFees).orElse(false));

        if (postDto.getStreet().isPresent() && postDto.getHouseNumber().isEmpty() && postDto.getDescription().isPresent()) {
            var description = postDto.getDescription().get();
            var street = postDto.getStreet().get();
            var streetPattern = Pattern.compile(String.format("%s ?(\\d{1,3}[A-Z]?)(\\s|(-\\d)|\\.|,|;|!|$)", street), Pattern.CASE_INSENSITIVE);
            var matcher = streetPattern.matcher(description);
            if (matcher.find()) {
                LOGGER.info("Found house number in description: {} of {} {}", matcher.group(1), postDto.getSource(), postDto.getExternalId());
                post.setHouseNumber(matcher.group(1).toUpperCase());
            }
        }

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
            "(ira mokestis)",
            "mokestis (jei butas",
            "\ntaikomas tarpininkavimas",
            "tiks vienkartinis tarpinink"
    );

    private static final List<Pattern> feePatterns = List.of(
            Pattern.compile("(agent|tarpinink|vienkart)\\S+ mokestis[\\s:-]{0,3}\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+\\s?\\S+ (agent|tarpinink|vienkart)\\S+ (tarp|mokest)\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\W(ira|bus) (taikoma(s|)|imama(s|)|vienkartinis|agent\\S+)( vienkartinis|) (agent|tarpinink|mokest)\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\Wtiks[^\\s\\w]?\\s?(bus|ira|) (taikoma(s|)|imama(s|))", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\W(ira |)(taikoma(s|)|imama(s|)|vienkartinis|sutarties)( sutarties|) sudar\\S+ mokestis", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(ui|ir) (ira |)(taikoma(s|)|imama(s|)) (vienkart|agent|tarpinink|mokest)\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(vienkartinis |)(agent|tarpinink)\\S+ mokest\\S+,? jei", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[^\\w\\s](\\s|)(taikoma(s|)|imama(s|)|vienkartinis|agent\\S+)( vienkartinis|) (agent|tarpinink|mokest)\\S+", Pattern.CASE_INSENSITIVE)
    );

    private boolean isWithFees(String description) {
        var descriptionSimplified = description;
        for (var entry : LETTER_REPLACE_MAP.entrySet()) {
            descriptionSimplified = descriptionSimplified.replace(entry.getKey(), entry.getValue());
        }

        for (var keyword : feeKeywords) {
            if (descriptionSimplified.contains(keyword)) {
                return true;
            }
        }

        for (var feePattern : feePatterns) {
            if (feePattern.matcher(descriptionSimplified).find()) {
                return true;
            }
        }

        return false;
    }
}
