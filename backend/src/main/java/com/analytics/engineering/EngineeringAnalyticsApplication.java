package com.analytics.engineering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import com.analytics.engineering.config.ExternalApiProperties;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(ExternalApiProperties.class)
public class EngineeringAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineeringAnalyticsApplication.class, args);
    }
}
