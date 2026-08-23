import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  icon: string;
  label: string;
  link: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  readonly lastUpdatedLabel = new Date().toLocaleString('pt-BR');

  readonly navItems: NavItem[] = [
    { icon: '🏠', label: 'Visão Geral', link: '/visao-geral' },
    { icon: '📁', label: 'Projetos', link: '/projetos' },
    { icon: '🚀', label: 'Releases', link: '/releases' },
    { icon: '🛡️', label: 'Qualidade', link: '/qualidade' },
    { icon: '📊', label: 'Tecnologias', link: '/tecnologias' },
    { icon: '👥', label: 'Times', link: '/times' },
    { icon: '📋', label: 'Dashboards', link: '/dashboards' },
    { icon: '⚙️', label: 'Configurações', link: '/configuracoes' },
  ];
}
