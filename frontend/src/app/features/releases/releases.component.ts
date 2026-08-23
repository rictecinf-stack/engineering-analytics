import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { catchError, of } from 'rxjs';

import { TopbarComponent, TopbarFilters } from '../../shared/components/topbar/topbar.component';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardFilters, ReleasesOverview } from '../../core/models/dashboard.models';

@Component({
  selector: 'app-releases',
  standalone: true,
  imports: [CommonModule, TopbarComponent, KpiCardComponent, BaseChartDirective],
  templateUrl: './releases.component.html',
  styleUrl: './releases.component.css',
})
export class ReleasesComponent implements OnInit {
  data: ReleasesOverview | null = null;
  loading = true;
  error = '';

  private currentFilters: DashboardFilters & { limit?: number } = { limit: 50 };

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
      environment: filters.environment || undefined,
      limit: 50,
    };
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService
      .getReleases(this.currentFilters)
      .pipe(
        catchError((err) => {
          this.error = 'Não foi possível carregar os dados do Jenkins. Verifique a API.';
          console.error(err);
          return of(null);
        })
      )
      .subscribe((releases) => {
        this.loading = false;
        if (!releases) return;
        this.data = releases;
        this.barData = {
          labels: releases.releasesPerMonth.map((p) => p.label),
          datasets: [
            {
              data: releases.releasesPerMonth.map((p) => p.value),
              backgroundColor: '#60a5fa',
              borderRadius: 4,
            },
          ],
        };
      });
  }

  statusLabel(status: string): string {
    return { SUCCESS: 'Sucesso', FAILURE: 'Falha', UNSTABLE: 'Instável' }[status] ?? status;
  }

  statusClass(status: string): string {
    return { SUCCESS: 'badge-green', FAILURE: 'badge-red', UNSTABLE: 'badge-orange' }[status] ?? '';
  }
}
