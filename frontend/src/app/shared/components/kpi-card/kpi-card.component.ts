import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Trend } from '../../../core/models/dashboard.models';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi-card.component.html',
  styleUrl: './kpi-card.component.css',
})
export class KpiCardComponent {
  @Input({ required: true }) icon = '📊';
  @Input({ required: true }) label = '';
  @Input({ required: true }) value = '';
  @Input() variationLabel = '';
  @Input() trend: Trend = 'flat';
  /** Cor de acento do card: blue | green | purple | orange | teal | red */
  @Input() accent: 'blue' | 'green' | 'purple' | 'orange' | 'teal' | 'red' = 'blue';

  get trendIcon(): string {
    if (this.trend === 'up') return '↑';
    if (this.trend === 'down') return '↓';
    return '';
  }
}
