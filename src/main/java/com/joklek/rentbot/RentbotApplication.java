package com.joklek.rentbot;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RentbotApplication {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
//        var posts = new AruodasScraper().getLatestPosts();
//        System.out.println(posts);
        SpringApplication.run(RentbotApplication.class, args);
    }
}
