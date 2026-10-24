import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { AcompanhamentoApi, CandidaturaApi } from '../../core/api.service';
import { FollowUpItem, FollowUps } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-follow-ups',
  standalone: true,
  imports: [DatePipe, NgTemplateOutlet, RouterLink, RotuloPipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="pagina">
      <div class="pagina-cabecalho">
        <h1>Follow-ups</h1>
        <button mat-icon-button aria-label="Recarregar" (click)="carregar()"><mat-icon>refresh</mat-icon></button>
      </div>

      @if (dados(); as d) {
        <section>
          <h2 class="titulo-secao vencidos"><mat-icon>error_outline</mat-icon> Vencidos ({{ d.vencidos.length }})</h2>
          @for (f of d.vencidos; track f.interacaoId) {
            <ng-container *ngTemplateOutlet="item; context: { $implicit: f }" />
          } @empty {
            <div class="cartao vazio">Nenhum follow-up vencido.</div>
          }
        </section>

        <section>
          <h2 class="titulo-secao"><mat-icon>event</mat-icon> Próximos 7 dias ({{ d.proximos.length }})</h2>
          @for (f of d.proximos; track f.interacaoId) {
            <ng-container *ngTemplateOutlet="item; context: { $implicit: f }" />
          } @empty {
            <div class="cartao vazio">Nada agendado para os próximos 7 dias.</div>
          }
        </section>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>

    <ng-template #item let-f>
      <div class="cartao follow-up">
        <div class="data" [class.atrasado]="f.diasParaVencer < 0">
          <strong>{{ f.dataFollowUp | date: 'dd/MM' }}</strong>
          <span>{{ prazo(f) }}</span>
        </div>
        <div class="corpo">
          <a [routerLink]="['/candidaturas', f.candidaturaId]" class="empresa">{{ f.empresa }}</a>
          <span class="texto-suave"> · {{ f.cargo }}</span>
          <span class="chip-status s-{{ f.status }}">{{ f.status | rotulo: 'status' }}</span>
          @if (f.proximoPasso) { <div><strong>Próximo passo:</strong> {{ f.proximoPasso }}</div> }
          @if (f.descricao) { <div class="texto-suave">{{ f.tipo | rotulo: 'interacao' }}: {{ f.descricao }}</div> }
        </div>
        <button mat-stroked-button (click)="concluir(f)"><mat-icon>check</mat-icon> Concluir</button>
      </div>
    </ng-template>
  `,
  styles: `
    section { margin-bottom: 24px; }
    .titulo-secao { display: flex; align-items: center; gap: 6px; font-size: 16px; font-weight: 500; margin: 0 0 8px; }
    .titulo-secao.vencidos { color: var(--cor-perigo); }
    .follow-up { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
    .data { display: flex; flex-direction: column; align-items: center; min-width: 64px; font-size: 12px; color: var(--cor-texto-suave); }
    .data strong { font-size: 18px; color: var(--cor-texto); }
    .data.atrasado, .data.atrasado strong { color: var(--cor-perigo); }
    .corpo { flex: 1 1 auto; display: grid; gap: 4px; min-width: 0; }
    .empresa { font-weight: 500; color: inherit; }
    .chip-status { justify-self: start; }
    @media (max-width: 599px) {
      .follow-up { flex-wrap: wrap; gap: 8px 12px; }
      .follow-up button { width: 100%; }
    }
  `,
})
export class FollowUpsComponent implements OnInit {
  private readonly api = inject(AcompanhamentoApi);
  private readonly candidaturaApi = inject(CandidaturaApi);
  private readonly notificacao = inject(Notificacao);

  readonly dados = signal<FollowUps | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.followUps().subscribe({ next: (d) => this.dados.set(d), error: (e) => this.notificacao.erro(e) });
  }

  concluir(f: FollowUpItem): void {
    this.candidaturaApi.concluirFollowUp(f.interacaoId).subscribe({
      next: () => {
        this.notificacao.ok(`Follow-up de ${f.empresa} concluído`);
        this.carregar();
      },
      error: (e) => this.notificacao.erro(e),
    });
  }

  prazo(f: FollowUpItem): string {
    const d = f.diasParaVencer;
    if (d === 0) {
      return 'hoje';
    }
    if (d === 1) {
      return 'amanhã';
    }
    return d < 0 ? `há ${-d} dia(s)` : `em ${d} dias`;
  }
}
