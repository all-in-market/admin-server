package com.example.allinmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AllInMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllInMarketApplication.class, args);
    }

}
