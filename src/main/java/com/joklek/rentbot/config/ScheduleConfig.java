package com.joklek.rentbot.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

@Profile("!test")
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @PostConstruct
    public void initScraper() {
        WebDriverManager.chromedriver().setup();
    }
}
