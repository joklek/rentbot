package com.joklek.rentbot.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class JsoupScraper implements Scraper {
    private static final Logger LOGGER = getLogger(JsoupScraper.class);
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Android 9; Mobile; rv:103.0) Gecko/103.0 Firefox/103.0";

    protected Optional<Document> getDocument(URI link) {
        try {
            return Optional.of(Jsoup.connect(link.toString())
                    .header("Host", link.getHost())
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true)
                    .get());
        } catch (IOException e) {
            LOGGER.error("Failed while fetching '{}'", link, e);
            return Optional.empty();
        }
    }
}
