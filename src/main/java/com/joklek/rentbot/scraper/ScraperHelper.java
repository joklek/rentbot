package com.joklek.rentbot.scraper;

import java.math.BigDecimal;
import java.util.Optional;

public class ScraperHelper {

    private ScraperHelper() {
    }

    public static Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            // TODO log bad parse
            return Optional.empty();
        }
    }

    public static Optional<BigDecimal> parseBigDecimal(String s) {
        try {
            return Optional.of(new BigDecimal(s.trim()));
        } catch (Exception e) {
            // TODO log bad parse
            return Optional.empty();
        }
    }
}
