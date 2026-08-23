package com.analytics.engineering.controller;

import com.analytics.engineering.dto.DashboardOverviewResponse;
import com.analytics.engineering.dto.DashboardOverviewResponse.ChartPoint;
import com.analytics.engineering.dto.ReleasesOverviewResponse;
import com.analytics.engineering.service.DashboardService;
import com.analytics.engineering.service.SonarService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SonarService sonarService;

    public DashboardController(DashboardService dashboardService, SonarService sonarService) {
        this.dashboardService = dashboardService;
        this.sonarService = sonarService;
    }

    /**
     * Payload consumido pela tela "Visão Geral" (KPIs, gráficos, tabela de tecnologias, rodapé).
     *
     * @param from          data inicial do filtro de período (ISO yyyy-MM-dd), opcional
     * @param to            data final do filtro de período (ISO yyyy-MM-dd), opcional
     * @param technologies  lista de tecnologias separadas por vírgula (ex: "Java,Python"), opcional
     */
    @GetMapping("/overview")
    public Mono<DashboardOverviewResponse> overview(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String technologies
    ) {
        return dashboardService.buildOverview(parseDate(from), parseDate(to), parseList(technologies));
    }

    /**
     * Payload consumido pela tela "Releases" (KPIs, gráfico mensal e lista de builds do Jenkins).
     *
     * @param environment "production" | "staging", opcional (todos por padrão)
     */
    @GetMapping("/releases")
    public Mono<ReleasesOverviewResponse> releases(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String environment
    ) {
        return dashboardService.buildReleasesOverview(limit, parseDate(from), parseDate(to), environment);
    }

    /** Série histórica de linhas de código para o gráfico "Evolução das linhas de código". */
    @GetMapping("/lines-evolution")
    public Mono<List<ChartPoint>> linesEvolution(
            @RequestParam String projectKey,
            @RequestParam(defaultValue = "2024-06-01") String from
    ) {
        return sonarService.linesOfCodeEvolution(projectKey, from);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
