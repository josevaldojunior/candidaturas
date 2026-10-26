import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DashboardApi } from '../../core/api.service';
import { Dashboard, Status } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, RotuloPipe, MatButtonModule, MatButtonToggleModule, MatIconModule,
    MatProgressBarModule, MatTooltipModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(DashboardApi);
  private readonly notificacao = inject(Notificacao);

  readonly d = signal<Dashboard | null>(null);
  /** Mesmos status que o card "Em andamento" conta (aguardando retorno ou com proposta). */
  readonly emAndamento: Status[] = ['CANDIDATADO', 'TRIAGEM_RH', 'TESTE_TECNICO', 'ENTREVISTA_TECNICA',
    'ENTREVISTA_FINAL', 'PROPOSTA'];
  /** Alterna entre gráficos e a mesma informação em tabela (acessibilidade). */
  readonly modo = signal<'grafico' | 'tabela'>('grafico');

  readonly maxFunil = computed(() => Math.max(1, ...(this.d()?.funil.map((e) => e.quantidade) ?? [])));
  readonly maxPlataforma = computed(() => Math.max(1, ...(this.d()?.porPlataforma.map((e) => e.quantidade) ?? [])));
  readonly maxSemana = computed(() => Math.max(1, ...(this.d()?.porSemana.map((e) => e.quantidade) ?? [])));
  readonly totalSemanas = computed(() => this.d()?.porSemana.reduce((s, e) => s + e.quantidade, 0) ?? 0);
  readonly enviadas = computed(() => this.d()?.funil.find((e) => e.etapa === 'CANDIDATADO')?.quantidade ?? 0);
  readonly entrevistas = computed(() => this.d()?.funil.find((e) => e.etapa === 'ENTREVISTA_TECNICA')?.quantidade ?? 0);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.resumo().subscribe({
      next: (d) => this.d.set(d),
      error: (e) => this.notificacao.erro(e, 'Erro ao carregar o dashboard'),
    });
  }

  largura(valor: number, max: number): string {
    return `${(valor / max) * 100}%`;
  }
}
