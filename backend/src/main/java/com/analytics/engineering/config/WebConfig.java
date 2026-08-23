package com.analytics.engineering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o acesso da SPA Angular (ex: http://localhost:4200) à API
 * e expõe os WebClients usados para falar com Sonar e Jenkins.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ExternalApiProperties properties;

    public WebConfig(ExternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getAllowedOrigin())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Bean
    public WebClient sonarWebClient() {
        ExternalApiProperties.Sonar sonar = properties.getSonar();
        return WebClient.builder()
                .baseUrl(sonar.getUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(sonar.getToken(), ""))
                .build();
    }

    @Bean
    public WebClient jenkinsWebClient() {
        ExternalApiProperties.Jenkins jenkins = properties.getJenkins();
        return WebClient.builder()
                .baseUrl(jenkins.getUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(jenkins.getUser(), jenkins.getApiToken()))
                .build();
    }
}
