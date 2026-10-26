import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AcompanhamentoApi, CandidaturaApi, DashboardApi } from '../../core/api.service';
import { Dashboard, Status } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { ETAPAS_TRAJETORIA } from '../../core/rotulos';
import { RotuloPipe } from '../../shared/rotulo.pipe';

/** Item de "Para fazer agora": uma pendência ou um follow-up. */
export interface Afazer {
  chave: string;
  candidaturaId: number;
  titulo: string;
  empresa: string;
  cargo: string;
  prazo: string;
  urgente: boolean;
  /** Para ordenar: pendências e atrasados primeiro, depois pelo prazo. */
  ordem: number;
}

const MAX_AFAZERES = 8;

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
  private readonly candidaturas = inject(CandidaturaApi);
  private readonly acompanhamento = inject(AcompanhamentoApi);
  private readonly notificacao = inject(Notificacao);

  readonly d = signal<Dashboard | null>(null);
  readonly afazeres = signal<Afazer[] | null>(null);
  readonly totalAfazeres = signal(0);
  /** Status que contam como "em andamento" (aguardando retorno ou com proposta). */
  readonly emAndamento: Status[] = ['CANDIDATADO', 'TRIAGEM_RH', 'TESTE_TECNICO', 'ENTREVISTA_TECNICA',
    'ENTREVISTA_FINAL', 'PROPOSTA'];
  /** Alterna entre gráficos e a mesma informação em tabela (acessibilidade). */
  readonly modo = signal<'grafico' | 'tabela'>('grafico');

  /** Etapas da trajetória com quantas candidaturas chegaram a cada uma. */
  readonly trajetoria = computed(() => {
    const funil = this.d()?.funil ?? [];
    return ETAPAS_TRAJETORIA.map((etapa) => funil.find((e) => e.etapa === etapa) ?? {
      etapa, quantidade: 0, conversaoDaAnterior: 0, percentualDoTotal: 0,
    });
  });
  readonly maxTrajetoria = computed(() => Math.max(1, ...this.trajetoria().map((e) => e.quantidade)));
  readonly maxPlataforma = computed(() => Math.max(1, ...(this.d()?.porPlataforma.map((e) => e.quantidade) ?? [])));
  readonly maxSemana = computed(() => Math.max(1, ...(this.d()?.porSemana.map((e) => e.quantidade) ?? [])));
  readonly totalSemanas = computed(() => this.d()?.porSemana.reduce((s, e) => s + e.quantidade, 0) ?? 0);
  readonly enviadas = computed(() => this.d()?.funil.find((e) => e.etapa === 'CANDIDATADO')?.quantidade ?? 0);
  readonly emInteresse = computed(() => this.d()?.porStatus.find((s) => s.chave === 'INTERESSE')?.quantidade ?? 0);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.resumo().subscribe({
      next: (d) => this.d.set(d),
      error: (e) => this.notificacao.erro(e, 'Erro ao carregar o dashboard'),
    });
    forkJoin({
      pendentes: this.candidaturas.listar({ pendentes: true }, 0, 50),
      followUps: this.acompanhamento.followUps(),
    }).subscribe({
      next: ({ pendentes, followUps }) => {
        const itens: Afazer[] = [
          ...pendentes.content.map((c) => ({
            chave: 'p' + c.id, candidaturaId: c.id, titulo: c.pendencia ?? 'Pendência', empresa: c.empresa,
            cargo: c.cargo, prazo: 'pendência', urgente: true, ordem: -1000,
          })),
          ...[...followUps.vencidos, ...followUps.proximos].map((f) => ({
            chave: 'f' + f.interacaoId, candidaturaId: f.candidaturaId,
            titulo: f.proximoPasso || f.descricao || 'Fazer follow-up', empresa: f.empresa, cargo: f.cargo,
            prazo: this.prazo(f.diasParaVencer), urgente: f.diasParaVencer <= 0, ordem: f.diasParaVencer,
          })),
        ].sort((a, b) => a.ordem - b.ordem);
        this.totalAfazeres.set(itens.length);
        this.afazeres.set(itens.slice(0, MAX_AFAZERES));
      },
      error: (e) => this.notificacao.erro(e, 'Erro ao carregar o que há para fazer'),
    });
  }

  prazo(dias: number): string {
    if (dias === 0) return 'hoje';
    if (dias === 1) return 'amanhã';
    if (dias === -1) return 'venceu ontem';
    return dias < 0 ? `venceu há ${-dias} dias` : `em ${dias} dias`;
  }

  largura(valor: number, max: number): string {
    return `${(valor / max) * 100}%`;
  }
}
