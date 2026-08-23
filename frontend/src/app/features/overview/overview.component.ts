import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { catchError, of } from 'rxjs';

import { TopbarComponent, TopbarFilters } from '../../shared/components/topbar/topbar.component';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardFilters, DashboardOverview } from '../../core/models/dashboard.models';

const DONUT_COLORS = ['#2f6fed', '#16a34a', '#7c3aed', '#f59e0b', '#0d9488', '#94a3b8'];

const ALL_TECHNOLOGIES = ['Java', 'TypeScript', 'JavaScript', 'Python', 'SQL', 'Outros'];

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, TopbarComponent, KpiCardComponent, BaseChartDirective],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css',
})
export class OverviewComponent implements OnInit {
  data: DashboardOverview | null = null;
  loading = true;
  error = '';

  readonly technologyOptions = ALL_TECHNOLOGIES;
  private currentFilters: DashboardFilters = {};

  donutData: ChartData<'doughnut'> = { labels: [], datasets: [{ data: [] }] };
  donutOptions: ChartConfiguration<'doughnut'>['options'] = {
    cutout: '68%',
    plugins: { legend: { display: false } },
  };

  lineData: ChartData<'line'> = { labels: [], datasets: [] };
  lineOptions: ChartConfiguration<'line'>['options'] = {
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: false } },
  };

  barData: ChartData<'bar'> = { labels: [], datasets: [] };
  barOptions: ChartConfiguration<'bar'>['options'] = {
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.load();
  }

  onFiltersChange(filters: TopbarFilters): void {
    this.currentFilters = {
      from: filters.from || undefined,
      to: filters.to || undefined,
      technologies: filters.technology ? [filters.technology] : undefined,
    };
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService
      .getOverview(this.currentFilters)
      .pipe(
        catchError((err) => {
          this.error = 'Não foi possível carregar os dados do Sonar/Jenkins. Verifique a API.';
          console.error(err);
          return of(null);
        })
      )
      .subscribe((overview) => {
        this.loading = false;
        if (!overview) return;
        this.data = overview;
        this.buildCharts(overview);
      });
  }

  private buildCharts(overview: DashboardOverview): void {
    this.donutData = {
      labels: overview.linesByTechnology.map((p) => p.label),
      datasets: [
        {
          data: overview.linesByTechnology.map((p) => p.value),
          backgroundColor: DONUT_COLORS,
          borderWidth: 0,
        },
      ],
    };

    this.lineData = {
      labels: overview.linesEvolution.map((p) => p.label),
      datasets: [
        {
          data: overview.linesEvolution.map((p) => p.value),
          borderColor: '#2f6fed',
          backgroundColor: 'rgba(47,111,237,0.12)',
          fill: true,
          tension: 0.35,
          pointRadius: 3,
        },
      ],
    };

    this.barData = {
      labels: overview.releasesPerMonth.map((p) => p.label),
      datasets: [
        {
          data: overview.releasesPerMonth.map((p) => p.value),
          backgroundColor: '#60a5fa',
          borderRadius: 4,
        },
      ],
    };
  }

  legendColor(index: number): string {
    return DONUT_COLORS[index % DONUT_COLORS.length];
  }
}
