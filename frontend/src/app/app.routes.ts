import { Routes } from '@angular/router';
import { OverviewComponent } from './features/overview/overview.component';
import { ReleasesComponent } from './features/releases/releases.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'visao-geral' },
  { path: 'visao-geral', component: OverviewComponent, title: 'Visão Geral | Engineering Analytics' },
  { path: 'releases', component: ReleasesComponent, title: 'Releases | Engineering Analytics' },
  { path: '**', redirectTo: 'visao-geral' },
];
