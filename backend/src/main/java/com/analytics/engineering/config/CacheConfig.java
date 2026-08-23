package com.analytics.engineering.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Os services (SonarService, JenkinsService) usam @Cacheable em métodos que retornam
 * Mono<...>. Para o Spring Cache conseguir cachear um Publisher reativo corretamente
 * (em vez de cachear o Mono "vazio" antes dele ser assinado), o CaffeineCacheManager
 * precisa estar em modo assíncrono (asyncCacheMode = true). O autoconfigure padrão do
 * Spring Boot não liga isso sozinho, por isso declaramos o bean manualmente aqui.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "sonarProjectMetrics",
                "sonarVulnerabilities",
                "sonarCriticalIssues",
                "jenkinsBuildStats",
                "jenkinsRecentBuilds"
        );
        manager.setAsyncCacheMode(true);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(2))
                .maximumSize(200));
        return manager;
    }
}
