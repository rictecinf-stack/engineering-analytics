package com.analytics.engineering.dto;

import com.analytics.engineering.dto.DashboardOverviewResponse.ChartPoint;
import com.analytics.engineering.dto.DashboardOverviewResponse.KpiValue;
import com.analytics.engineering.dto.DashboardOverviewResponse.ReleaseRow;

import java.util.List;

public record ReleasesOverviewResponse(
        KpiValue totalReleases,
        KpiValue successRate,
        KpiValue averageLeadTime,
        KpiValue deployFailures,
        List<ChartPoint> releasesPerMonth,
        List<ReleaseRow> releases
) {}
