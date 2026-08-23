package com.analytics.engineering.service;

import com.analytics.engineering.client.SonarClient;
import com.analytics.engineering.dto.DashboardOverviewResponse.ChartPoint;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Traduz as respostas cruas do SonarQube em dados prontos para o dashboard:
 * cobertura média, linhas de código por linguagem, vulnerabilidades, issues críticas,
 * duplicação de código e débito técnico.
 */
@Service
public class SonarService {

    private static final String OVERVIEW_METRICS =
            "coverage,ncloc,ncloc_language_distribution,duplicated_lines_density,"
                    + "sqale_index,vulnerabilities,bugs,code_smells";

    private final SonarClient sonarClient;

    public SonarService(SonarClient sonarClient) {
        this.sonarClient = sonarClient;
    }

    /** Lista todos os projetos do Sonar já com as métricas de qualidade agregadas. */
    @Cacheable("sonarProjectMetrics")
    public Mono<List<ProjectMetrics>> fetchProjectMetrics() {
        return sonarClient.searchProjects(1, 500)
                .flatMapMany(root -> Flux.fromIterable(root.path("components")))
                .flatMap(project -> {
                    String key = project.path("key").asText();
                    String name = project.path("name").asText(key);
                    return sonarClient.componentMeasures(key, OVERVIEW_METRICS)
                            .map(measures -> toProjectMetrics(name, measures));
                },8)
                .collectList();
    }

    /** Total de vulnerabilidades abertas (BLOCKER/CRITICAL) em todos os projetos. */
    @Cacheable("sonarVulnerabilities")
    public Mono<Integer> countOpenVulnerabilities() {
        return sonarClient.searchIssues("BLOCKER,CRITICAL", "VULNERABILITY", 1)
                .map(root -> root.path("total").asInt(0));
    }

    /** Issues críticas (bugs + code smells + vulnerabilidades com severidade BLOCKER). */
    @Cacheable("sonarCriticalIssues")
    public Mono<Integer> countCriticalIssues() {
        return sonarClient.searchIssues("BLOCKER", "BUG,VULNERABILITY,CODE_SMELL", 1)
                .map(root -> root.path("total").asInt(0));
    }

    /** Série histórica de linhas de código de um projeto de referência, para o gráfico de evolução. */
    public Mono<List<ChartPoint>> linesOfCodeEvolution(String projectKey, String fromDate) {
        return sonarClient.measuresHistory(projectKey, "ncloc", fromDate)
                .map(root -> {
                    List<ChartPoint> points = new ArrayList<>();
                    Iterator<JsonNode> it = root.path("measures").elements();
                    while (it.hasNext()) {
                        JsonNode measure = it.next();
                        for (JsonNode h : measure.path("history")) {
                            String date = h.path("date").asText().substring(0, 7); // yyyy-MM
                            double value = h.path("value").asDouble(0);
                            points.add(new ChartPoint(date, value));
                        }
                    }
                    return points;
                });
    }

    private ProjectMetrics toProjectMetrics(String name, JsonNode measuresRoot) {
        Map<String, String> metrics = new java.util.HashMap<>();
        for (JsonNode m : measuresRoot.path("component").path("measures")) {
            metrics.put(m.path("metric").asText(), m.path("value").asText("0"));
        }
        return new ProjectMetrics(
                name,
                parseDouble(metrics.get("coverage")),
                parseDouble(metrics.get("ncloc")),
                metrics.getOrDefault("ncloc_language_distribution", ""),
                parseDouble(metrics.get("duplicated_lines_density")),
                parseLong(metrics.get("sqale_index")), // minutos de débito técnico
                parseInt(metrics.get("vulnerabilities")),
                parseInt(metrics.get("bugs")),
                parseInt(metrics.get("code_smells"))
        );
    }

    private double parseDouble(String v) {
        try { return v == null ? 0 : Double.parseDouble(v); } catch (NumberFormatException e) { return 0; }
    }

    private long parseLong(String v) {
        try { return v == null ? 0 : Long.parseLong(v); } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String v) {
        try { return v == null ? 0 : Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    public record ProjectMetrics(
            String name,
            double coverage,
            double linesOfCode,
            String languageDistribution, // formato Sonar: "java=123;js=456"
            double duplicationDensity,
            long technicalDebtMinutes,
            int vulnerabilities,
            int bugs,
            int codeSmells
    ) {}
}
