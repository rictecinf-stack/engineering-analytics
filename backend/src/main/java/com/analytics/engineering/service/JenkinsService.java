package com.analytics.engineering.service;

import com.analytics.engineering.client.JenkinsClient;
import com.analytics.engineering.dto.DashboardOverviewResponse.ChartPoint;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Traduz as respostas cruas do Jenkins em dados prontos para o dashboard:
 * total de releases, releases por mês, última release, lead time médio,
 * deployments e falhas de deploy.
 */
@Service
public class JenkinsService {

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final JenkinsClient jenkinsClient;

    public JenkinsService(JenkinsClient jenkinsClient) {
        this.jenkinsClient = jenkinsClient;
    }

    /** Todos os builds conhecidos, já convertidos e cacheados por 2 minutos. */
    @Cacheable("jenkinsBuildStats")
    public Mono<BuildStats> fetchBuildStats() {
        return jenkinsClient.listJobs().map(this::parseAndAggregate);
    }

    /**
     * Aplica um filtro de data (e opcionalmente ambiente) sobre os builds já
     * carregados, recalculando totais/mensal/lead time/falhas só para o subconjunto.
     * Não bate no Jenkins de novo — reaproveita o cache de fetchBuildStats().
     */
    public Mono<BuildStats> fetchBuildStats(LocalDate from, LocalDate to, String environment) {
        return fetchBuildStats().map(stats -> {
            List<BuildEntry> filtered = stats.allBuilds().stream()
                    .filter(b -> withinRange(b.timestamp(), from, to))
                    .filter(b -> environment == null || environment.isBlank()
                            || environment.equalsIgnoreCase(b.environment()))
                    .toList();
            return aggregate(filtered);
        });
    }

    private boolean withinRange(Instant timestamp, LocalDate from, LocalDate to) {
        LocalDate date = timestamp.atZone(ZONE).toLocalDate();
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private BuildStats parseAndAggregate(JsonNode root) {
        List<BuildEntry> allBuilds = new ArrayList<>();

        for (JsonNode job : root.path("jobs")) {
            String jobName = job.path("name").asText();
            for (JsonNode build : job.path("builds")) {
                long ts = build.path("timestamp").asLong(0);
                if (ts == 0) continue;
                Instant instant = Instant.ofEpochMilli(ts);
                String result = build.path("result").asText("SUCCESS");
                long duration = build.path("duration").asLong(0);
                int number = build.path("number").asInt(0);
                allBuilds.add(new BuildEntry(jobName, number, instant, result, duration, inferEnvironment(jobName, number)));
            }
        }

        return aggregate(allBuilds);
    }

    /**
     * A REST API padrão do Jenkins não expõe "ambiente" como campo nativo do build.
     * Aqui inferimos de forma determinística (mesmo job+número sempre cai no mesmo
     * ambiente) só para termos uma dimensão filtrável na demo. Num cenário real, isso
     * viria de um parâmetro de build, de uma convenção de nome de job/pasta, ou de um
     * sistema de deploy separado — vale trocar por uma fonte de dado real quando
     * migrar para o Jenkins de produção.
     */
    private String inferEnvironment(String jobName, int buildNumber) {
        int hash = Math.abs((jobName + buildNumber).hashCode());
        return hash % 5 == 0 ? "staging" : "production";
    }

    private BuildStats aggregate(List<BuildEntry> builds) {
        Map<String, Integer> releasesPerMonth = new TreeMap<>();
        for (BuildEntry b : builds) {
            String monthKey = MONTH_KEY.format(b.timestamp().atZone(ZONE));
            releasesPerMonth.merge(monthKey, 1, Integer::sum);
        }

        List<BuildEntry> sorted = new ArrayList<>(builds);
        sorted.sort(Comparator.comparing(BuildEntry::timestamp).reversed());

        long totalBuilds = sorted.size();
        long failed = sorted.stream().filter(b -> "FAILURE".equals(b.result())).count();
        double avgDurationMs = sorted.stream().mapToLong(BuildEntry::durationMs).average().orElse(0);
        double avgLeadTimeDays = avgDurationMs / (1000.0 * 60 * 60 * 24);

        BuildEntry last = sorted.isEmpty() ? null : sorted.get(0);

        List<ChartPoint> monthlyPoints = new ArrayList<>();
        releasesPerMonth.forEach((month, count) -> monthlyPoints.add(new ChartPoint(month, count)));

        return new BuildStats(
                totalBuilds,
                monthlyPoints,
                last,
                avgLeadTimeDays,
                (int) totalBuilds,
                (int) failed,
                sorted
        );
    }

    public record BuildEntry(
            String jobName,
            int number,
            Instant timestamp,
            String result,
            long durationMs,
            String environment
    ) {}

    public record BuildStats(
            long totalReleases,
            List<ChartPoint> releasesPerMonth,
            BuildEntry lastRelease,
            double averageLeadTimeDays,
            int deployments,
            int deployFailures,
            List<BuildEntry> allBuilds
    ) {}
}
