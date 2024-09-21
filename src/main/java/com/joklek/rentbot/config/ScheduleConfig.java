package com.joklek.rentbot.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;


@Profile("!test")
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @PostConstruct
    public void initScraper() {
        WebDriverManager.firefoxdriver().setup();
    }
}
