import { CdkDragDrop, DragDropModule, transferArrayItem } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { CandidaturaApi } from '../../core/api.service';
import { CandidaturaResumo, ColunaKanban, Status } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { ROTULO_STATUS } from '../../core/rotulos';
import { RotuloPipe } from '../../shared/rotulo.pipe';

/** Status encerrados ficam ocultos por padrão para o quadro não poluir. */
const ENCERRADOS: Status[] = ['RECUSADA', 'SEM_RETORNO'];

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [DragDropModule, DatePipe, FormsModule, RouterLink, RotuloPipe, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSlideToggleModule, MatTooltipModule],
  templateUrl: './kanban.component.html',
  styleUrl: './kanban.component.scss',
})
export class KanbanComponent implements OnInit {
  private readonly api = inject(CandidaturaApi);
  private readonly notificacao = inject(Notificacao);

  readonly colunas = signal<ColunaKanban[]>([]);
  readonly carregando = signal(false);
  readonly mostrarEncerradas = signal(false);
  readonly busca = signal('');
  readonly soPendentes = signal(false);

  readonly colunasVisiveis = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    return this.colunas()
      .filter((c) => this.mostrarEncerradas() || !ENCERRADOS.includes(c.status))
      .map((c) => ({
        ...c,
        // a lista original é mantida para o drag-drop; o filtro só esconde cartões
        visiveis: c.candidaturas.filter(
          (x) => (!this.soPendentes() || !!x.pendencia)
            && (!termo || `${x.empresa} ${x.cargo} ${x.stack.join(' ')}`.toLowerCase().includes(termo)),
        ),
      }));
  });

  readonly idsListas = computed(() => this.colunasVisiveis().map((c) => `coluna-${c.status}`));

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.api.kanban().subscribe({
      next: (c) => {
        this.colunas.set(c);
        this.carregando.set(false);
      },
      error: (e) => {
        this.carregando.set(false);
        this.notificacao.erro(e, 'Erro ao carregar o quadro');
      },
    });
  }

  soltar(evento: CdkDragDrop<CandidaturaResumo[], CandidaturaResumo[], CandidaturaResumo>, destino: Status): void {
    // a ordem dentro da coluna não é persistida: só interessa a troca de coluna
    if (evento.previousContainer === evento.container) {
      return;
    }
    const item = evento.item.data;
    // atualização otimista: move o cartão já e recarrega o quadro se a API recusar
    const indiceOrigem = evento.previousContainer.data.indexOf(item);
    transferArrayItem(evento.previousContainer.data, evento.container.data, indiceOrigem, 0);
    item.status = destino;
    this.colunas.update((c) => [...c]);

    this.api.mudarStatus(item.id, destino).subscribe({
      next: (atualizada) => {
        item.ultimaInteracaoEm = atualizada.ultimaInteracaoEm;
        // sair de Interesse conclui a pendência no backend: o selo some do cartão
        item.pendencia = atualizada.pendencia;
        this.colunas.update((c) => [...c]);
        this.notificacao.ok(`${item.empresa} → ${ROTULO_STATUS[destino]}`);
      },
      error: (e) => {
        this.notificacao.erro(e, 'Não foi possível mudar o status');
        this.carregar();
      },
    });
  }
}
