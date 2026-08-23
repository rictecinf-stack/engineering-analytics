package com.analytics.engineering.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Encapsula as chamadas cruas à REST API do Jenkins (JSON API nativa, via /api/json).
 * Docs: https://www.jenkins.io/doc/book/using/remote-access-api/
 */
@Component
public class JenkinsClient {

    private final WebClient jenkinsWebClient;

    public JenkinsClient(WebClient jenkinsWebClient) {
        this.jenkinsWebClient = jenkinsWebClient;
    }

    /** Lista todos os jobs (releases/pipelines) com seus últimos builds. */
    public Mono<JsonNode> listJobs() {
        return jenkinsWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/json")
                        .queryParam("tree",
                                "jobs[name,url,color,lastBuild[number,timestamp,result,duration],"
                                        + "builds[number,timestamp,result,duration]]")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /** Detalhe de builds de um job específico (para calcular releases/mês, lead time, falhas). */
    public Mono<JsonNode> jobBuilds(String jobName) {
        return jenkinsWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/job/{jobName}/api/json")
                        .queryParam("tree", "builds[number,timestamp,result,duration,building]")
                        .build(jobName))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /** Status atual da fila de builds (deployments em andamento). */
    public Mono<JsonNode> queueInfo() {
        return jenkinsWebClient.get()
                .uri("/queue/api/json")
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
