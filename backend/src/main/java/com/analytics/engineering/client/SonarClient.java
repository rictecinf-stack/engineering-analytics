package com.analytics.engineering.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Encapsula as chamadas cruas à REST API do SonarQube/SonarCloud.
 * Docs: https://docs.sonarsource.com/sonarqube/latest/extension-guide/web-api/
 */
@Component
public class SonarClient {

    private final WebClient sonarWebClient;

    public SonarClient(WebClient sonarWebClient) {
        this.sonarWebClient = sonarWebClient;
    }

    /** Lista de projetos cadastrados no Sonar. */
    public Mono<JsonNode> searchProjects(int page, int pageSize) {
        return sonarWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/projects/search")
                        .queryParam("p", page)
                        .queryParam("ps", pageSize)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /**
     * Métricas agregadas de um componente (projeto).
     * metricKeys ex: "coverage,duplicated_lines_density,sqale_index,vulnerabilities,
     * ncloc,ncloc_language_distribution,bugs,code_smells"
     */
    public Mono<JsonNode> componentMeasures(String projectKey, String metricKeys) {
        return sonarWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/measures/component")
                        .queryParam("component", projectKey)
                        .queryParam("metricKeys", metricKeys)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /** Issues abertas filtradas por severidade (usado para vulnerabilidades / issues críticas). */
    public Mono<JsonNode> searchIssues(String severities, String types, int pageSize) {
        return sonarWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/issues/search")
                        .queryParam("severities", severities)
                        .queryParam("types", types)
                        .queryParam("resolved", "false")
                        .queryParam("ps", pageSize)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /** Histórico de uma métrica (usado no gráfico "Evolução das linhas de código" / cobertura). */
    public Mono<JsonNode> measuresHistory(String projectKey, String metric, String from) {
        return sonarWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/measures/search_history")
                        .queryParam("component", projectKey)
                        .queryParam("metrics", metric)
                        .queryParam("from", from)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
