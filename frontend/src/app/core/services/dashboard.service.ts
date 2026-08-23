import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChartPoint,
  DashboardFilters,
  DashboardOverview,
  ReleasesOverview,
} from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${environment.apiUrl}/api/dashboard`;

  constructor(private readonly http: HttpClient) {}

  getOverview(filters?: DashboardFilters): Observable<DashboardOverview> {
    let params = new HttpParams();
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);
    if (filters?.technologies?.length) params = params.set('technologies', filters.technologies.join(','));
    return this.http.get<DashboardOverview>(`${this.baseUrl}/overview`, { params });
  }

  getReleases(filters?: DashboardFilters & { limit?: number }): Observable<ReleasesOverview> {
    let params = new HttpParams();
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);
    if (filters?.environment) params = params.set('environment', filters.environment);
    if (filters?.limit) params = params.set('limit', filters.limit);
    return this.http.get<ReleasesOverview>(`${this.baseUrl}/releases`, { params });
  }

  getLinesEvolution(projectKey: string, from: string): Observable<ChartPoint[]> {
    return this.http.get<ChartPoint[]>(`${this.baseUrl}/lines-evolution`, {
      params: { projectKey, from },
    });
  }
}
