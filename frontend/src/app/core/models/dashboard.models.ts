export type Trend = 'up' | 'down' | 'flat';

export interface KpiValue {
  value: string;
  variationLabel: string;
  trend: Trend;
}

export interface SummaryCards {
  projects: KpiValue;
  releases: KpiValue;
  linesOfCode: KpiValue;
  technologies: KpiValue;
  averageCoverage: KpiValue;
  vulnerabilities: KpiValue;
  criticalIssues: KpiValue;
}

export interface ChartPoint {
  label: string;
  value: number;
}

export interface QualityIndicator {
  name: string;
  value: string;
  variationLabel: string;
  trend: Trend;
  sparkline: number[];
}

export interface TechnologyRow {
  name: string;
  projects: number;
  linesOfCode: string;
  releases: number;
  coveragePercent: number;
  vulnerabilities: number;
}

export interface FooterStats {
  lastReleaseVersion: string;
  lastReleaseDate: string;
  averageLeadTimeDays: string;
  leadTimeVariation: string;
  deployments: number;
  deploymentsVariation: string;
  deployFailures: number;
  deployFailuresVariation: string;
  monitoredEnvironments: number;
}

export interface ReleaseRow {
  jobName: string;
  version: string;
  environment: string;
  status: string; // SUCCESS | FAILURE | UNSTABLE
  date: string;
  durationLabel: string;
}

export interface ReleasesOverview {
  totalReleases: KpiValue;
  successRate: KpiValue;
  averageLeadTime: KpiValue;
  deployFailures: KpiValue;
  releasesPerMonth: ChartPoint[];
  releases: ReleaseRow[];
}

export interface DashboardFilters {
  from?: string; // ISO yyyy-MM-dd
  to?: string;
  technologies?: string[];
  environment?: string;
}

export interface DashboardOverview {
  summary: SummaryCards;
  linesByTechnology: ChartPoint[];
  linesEvolution: ChartPoint[];
  releasesPerMonth: ChartPoint[];
  qualityIndicators: QualityIndicator[];
  technologies: TechnologyRow[];
  footerStats: FooterStats;
  lastUpdated: string;
}
