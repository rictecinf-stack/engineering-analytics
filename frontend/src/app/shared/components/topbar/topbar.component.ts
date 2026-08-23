import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface TopbarFilters {
  from: string;
  to: string;
  technology: string; // '' = todas
  environment: string; // '' = todos
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css',
})
export class TopbarComponent implements OnChanges {
  @Input({ required: true }) title = '';
  @Input({ required: true }) subtitle = '';

  /** Lista de tecnologias disponíveis para o select (nomes exibidos, ex: "Java"). Vazio = esconde o filtro. */
  @Input() technologyOptions: string[] = [];
  /** Mostra o filtro de ambiente (só faz sentido na tela de Releases, já que é dado do Jenkins). */
  @Input() showEnvironmentFilter = false;
  /** Mostra o filtro de tecnologia (só faz sentido na Visão Geral, dado do Sonar). */
  @Input() showTechnologyFilter = false;
  /** Valores iniciais dos filtros (ex: para refletir defaults vindos do componente pai). */
  @Input() initialFrom = '';
  @Input() initialTo = '';

  @Output() filtersChange = new EventEmitter<TopbarFilters>();
  @Output() refresh = new EventEmitter<void>();

  from = '';
  to = '';
  technology = '';
  environmentValue = '';

  readonly environmentOptions = [
    { value: '', label: 'Todos os ambientes' },
    { value: 'production', label: 'Production' },
    { value: 'staging', label: 'Staging' },
  ];

  ngOnChanges(): void {
    if (this.initialFrom) this.from = this.initialFrom;
    if (this.initialTo) this.to = this.initialTo;
  }

  onFilterChange(): void {
    this.filtersChange.emit({
      from: this.from,
      to: this.to,
      technology: this.technology,
      environment: this.environmentValue,
    });
  }

  onRefresh(): void {
    this.refresh.emit();
  }
}
