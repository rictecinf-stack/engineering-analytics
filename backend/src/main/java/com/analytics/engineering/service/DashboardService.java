package com.analytics.engineering.service;

import com.analytics.engineering.dto.DashboardOverviewResponse;
import com.analytics.engineering.dto.DashboardOverviewResponse.*;
import com.analytics.engineering.dto.ReleasesOverviewResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Combina os dados do Sonar (qualidade/código) com os dados do Jenkins (releases/deploys)
 * no formato exato consumido pelas telas do Angular, já aplicando os filtros vindos
 * da topbar (período, tecnologia, ambiente).
 */
@Service
public class DashboardService {

    private final SonarService sonarService;
    private final JenkinsService jenkinsService;

    public DashboardService(SonarService sonarService, JenkinsService jenkinsService) {
        this.sonarService = sonarService;
        this.jenkinsService = jenkinsService;
    }

    /**
     * @param from          filtra projetos/releases a partir desta data (inclusive); null = sem limite inferior
     * @param to            filtra projetos/releases até esta data (inclusive); null = sem limite superior
     * @param technologies  filtra o dashboard só para essas tecnologias (nomes "bonitos": Java, TypeScript, ...);
     *                      vazio/null = todas
     */
    public Mono<DashboardOverviewResponse> buildOverview(LocalDate from, LocalDate to, List<String> technologies) {
        Mono<List<SonarService.ProjectMetrics>> projectsMono = sonarService.fetchProjectMetrics()
                .map(projects -> filterByTechnology(projects, technologies));
        Mono<Integer> vulnerabilitiesMono = sonarService.countOpenVulnerabilities();
        Mono<Integer> criticalIssuesMono = sonarService.countCriticalIssues();
        Mono<JenkinsService.BuildStats> buildStatsMono = jenkinsService.fetchBuildStats(from, to, null);

        return Mono.zip(projectsMono, vulnerabilitiesMono, criticalIssuesMono, buildStatsMono)
                .map(tuple -> assemble(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    private List<SonarService.ProjectMetrics> filterByTechnology(
            List<SonarService.ProjectMetrics> projects, List<String> technologies
    ) {
        if (technologies == null || technologies.isEmpty()) return projects;
        Set<String> wanted = technologies.stream().map(String::trim).collect(Collectors.toSet());
        return projects.stream()
                .filter(p -> wanted.contains(mainLanguage(p.languageDistribution())))
                .collect(Collectors.toList());
    }

    private DashboardOverviewResponse assemble(
            List<SonarService.ProjectMetrics> projects,
            int vulnerabilities,
            int criticalIssues,
            JenkinsService.BuildStats buildStats
    ) {
        double avgCoverage = projects.stream().mapToDouble(SonarService.ProjectMetrics::coverage).average().orElse(0);
        double avgDuplication = projects.stream().mapToDouble(SonarService.ProjectMetrics::duplicationDensity).average().orElse(0);
        double totalLoc = projects.stream().mapToDouble(SonarService.ProjectMetrics::linesOfCode).sum();
        long totalDebtMinutes = projects.stream().mapToLong(SonarService.ProjectMetrics::technicalDebtMinutes).sum();

        Map<String, Double> locByLanguage = new LinkedHashMap<>();
        for (SonarService.ProjectMetrics p : projects) {
            for (String pair : p.languageDistribution().split(";")) {
                if (pair.isBlank()) continue;
                String[] parts = pair.split("=");
                if (parts.length != 2) continue;
                locByLanguage.merge(prettyLanguage(parts[0]), Double.parseDouble(parts[1]), Double::sum);
            }
        }

        List<ChartPoint> linesByTechnology = locByLanguage.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        SummaryCards summary = new SummaryCards(
                new KpiValue(String.valueOf(projects.size()), "projetos no filtro atual", "flat"),
                new KpiValue(String.valueOf(buildStats.totalReleases()), "no período filtrado", "flat"),
                new KpiValue(formatMillions(totalLoc), "no filtro atual", "flat"),
                new KpiValue(String.valueOf(locByLanguage.size()), "no filtro atual", "flat"),
                new KpiValue(String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", avgCoverage), "no filtro atual", "flat"),
                new KpiValue(String.valueOf(vulnerabilities), "no filtro atual", "flat"),
                new KpiValue(String.valueOf(criticalIssues), "no filtro atual", "flat")
        );

        List<QualityIndicator> qualityIndicators = List.of(
                new QualityIndicator("Cobertura de Código",
                        String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", avgCoverage),
                        "no filtro atual", "flat", List.of()),
                new QualityIndicator("Duplicação de Código",
                        String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", avgDuplication),
                        "no filtro atual", "flat", List.of()),
                new QualityIndicator("Debt Técnico",
                        (totalDebtMinutes / 60) + "h",
                        "no filtro atual", "flat", List.of()),
                new QualityIndicator("Vulnerabilidades",
                        String.valueOf(vulnerabilities),
                        "no filtro atual", "flat", List.of()),
                new QualityIndicator("Issues Críticas",
                        String.valueOf(criticalIssues),
                        "no filtro atual", "flat", List.of())
        );

        List<TechnologyRow> technologies = projects.stream()
                .collect(Collectors.groupingBy(p -> mainLanguage(p.languageDistribution())))
                .entrySet().stream()
                .map(e -> new TechnologyRow(
                        e.getKey(),
                        e.getValue().size(),
                        formatMillions(e.getValue().stream().mapToDouble(SonarService.ProjectMetrics::linesOfCode).sum()),
                        0,
                        e.getValue().stream().mapToDouble(SonarService.ProjectMetrics::coverage).average().orElse(0),
                        e.getValue().stream().mapToInt(SonarService.ProjectMetrics::vulnerabilities).sum()
                ))
                .sorted(Comparator.comparingInt(TechnologyRow::projects).reversed())
                .collect(Collectors.toList());

        FooterStats footerStats = new FooterStats(
                buildStats.lastRelease() != null ? "v-" + buildStats.lastRelease().jobName() : "-",
                buildStats.lastRelease() != null
                        ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        .format(buildStats.lastRelease().timestamp().atZone(ZoneId.systemDefault()))
                        : "-",
                String.format(Locale.forLanguageTag("pt-BR"), "%.1f dias", buildStats.averageLeadTimeDays()),
                "no período filtrado",
                buildStats.deployments(),
                "no período filtrado",
                buildStats.deployFailures(),
                "no período filtrado",
                technologies.size()
        );

        return new DashboardOverviewResponse(
                summary,
                linesByTechnology,
                List.of(), // preenchido via /api/dashboard/lines-evolution com um projeto de referência
                buildStats.releasesPerMonth(),
                qualityIndicators,
                technologies,
                footerStats,
                OffsetDateTime.now()
        );
    }

    /**
     * Monta o payload consumido pela tela "Releases": KPIs, gráfico mensal e a lista de builds.
     *
     * @param environment "production" | "staging" | null (todos)
     */
    public Mono<ReleasesOverviewResponse> buildReleasesOverview(int limit, LocalDate from, LocalDate to, String environment) {
        return jenkinsService.fetchBuildStats(from, to, environment).map(stats -> {
            long total = stats.totalReleases();
            long failed = stats.deployFailures();
            double successRate = total == 0 ? 0 : (100.0 * (total - failed) / total);

            List<ReleaseRow> rows = stats.allBuilds().stream()
                    .limit(limit)
                    .map(this::toReleaseRow)
                    .collect(Collectors.toList());

            return new ReleasesOverviewResponse(
                    new KpiValue(String.valueOf(total), "no filtro atual", "flat"),
                    new KpiValue(String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", successRate), "no filtro atual", "flat"),
                    new KpiValue(String.format(Locale.forLanguageTag("pt-BR"), "%.1f dias", stats.averageLeadTimeDays()), "no filtro atual", "flat"),
                    new KpiValue(String.valueOf(failed), "no filtro atual", "flat"),
                    stats.releasesPerMonth(),
                    rows
            );
        });
    }

    private ReleaseRow toReleaseRow(JenkinsService.BuildEntry b) {
        String durationLabel = String.format(Locale.forLanguageTag("pt-BR"), "%.1f min", b.durationMs() / 60000.0);
        String date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(b.timestamp().atZone(ZoneId.systemDefault()));
        return new ReleaseRow(
                b.jobName(),
                "v-" + b.jobName() + "." + b.number(),
                b.environment(),
                b.result(),
                date,
                durationLabel
        );
    }

    private String prettyLanguage(String sonarKey) {
        return switch (sonarKey) {
            case "java" -> "Java";
            case "js" -> "JavaScript";
            case "ts" -> "TypeScript";
            case "py" -> "Python";
            case "sql" -> "SQL";
            default -> "Outros";
        };
    }

    private String mainLanguage(String distribution) {
        if (distribution == null || distribution.isBlank()) return "Outros";
        return Arrays.stream(distribution.split(";"))
                .map(pair -> pair.split("="))
                .filter(p -> p.length == 2)
                .max(Comparator.comparingDouble(p -> Double.parseDouble(p[1])))
                .map(p -> prettyLanguage(p[0]))
                .orElse("Outros");
    }

    private String formatMillions(double loc) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f M", loc / 1_000_000.0);
    }
}
