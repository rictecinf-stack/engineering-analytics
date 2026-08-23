package com.analytics.engineering.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DashboardOverviewResponse(
        SummaryCards summary,
        List<ChartPoint> linesByTechnology,
        List<ChartPoint> linesEvolution,
        List<ChartPoint> releasesPerMonth,
        List<QualityIndicator> qualityIndicators,
        List<TechnologyRow> technologies,
        FooterStats footerStats,
        OffsetDateTime lastUpdated
) {
    public record SummaryCards(
            KpiValue projects,
            KpiValue releases,
            KpiValue linesOfCode,
            KpiValue technologies,
            KpiValue averageCoverage,
            KpiValue vulnerabilities,
            KpiValue criticalIssues
    ) {}

    public record KpiValue(
            String value,
            String variationLabel,
            String trend // "up" | "down" | "flat"
    ) {}

    public record ChartPoint(String label, double value) {}

    public record QualityIndicator(
            String name,
            String value,
            String variationLabel,
            String trend,
            List<Double> sparkline
    ) {}

    public record TechnologyRow(
            String name,
            int projects,
            String linesOfCode,
            int releases,
            double coveragePercent,
            int vulnerabilities
    ) {}

    public record ReleaseRow(
            String jobName,
            String version,
            String environment,
            String status, // SUCCESS | FAILURE | UNSTABLE | RUNNING
            String date,
            String durationLabel
    ) {}

    public record FooterStats(
            String lastReleaseVersion,
            String lastReleaseDate,
            String averageLeadTimeDays,
            String leadTimeVariation,
            int deployments,
            String deploymentsVariation,
            int deployFailures,
            String deployFailuresVariation,
            int monitoredEnvironments
    ) {}
}
