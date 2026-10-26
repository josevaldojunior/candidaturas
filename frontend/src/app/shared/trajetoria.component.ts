import { Component, computed, input } from '@angular/core';
import { Status } from '../core/models';
import { ETAPAS_TRAJETORIA, ROTULO_ETAPA_CURTO, ROTULO_STATUS } from '../core/rotulos';

type Categoria = 'interesse' | 'andamento' | 'chegada' | 'encerrada';

/**
 * Trajetória da candidatura: as 7 etapas do processo, preenchidas até onde ela chegou.
 * "mini" cabe numa linha de tabela ou num cartão; "completa" mostra o nome de cada etapa.
 * Encerradas (recusada, sem retorno) ficam em cinza até a etapa alcançada, quando conhecida.
 */
@Component({
  selector: 'app-trajetoria',
  standalone: true,
  template: `
    @if (modo() === 'mini') {
      <span class="trilha" role="img" [attr.aria-label]="descricao()">
        @for (e of etapas; track e; let i = $index) { <i [class.cheio]="i < posicao()"></i> }
      </span>
      @if (comRotulo()) { <span class="rotulo">{{ rotulo() }}</span> }
    } @else {
      <ol class="completa" [attr.aria-label]="descricao()">
        @for (e of etapas; track e; let i = $index) {
          <li [class.cheio]="i < posicao()" [class.atual]="i === posicao() - 1 && categoria() !== 'encerrada'"
              [attr.aria-current]="i === posicao() - 1 ? 'step' : null">
            <i></i><span>{{ rotulosCurtos[e] }}</span>
          </li>
        }
      </ol>
    }
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; --cor-cheio: var(--cor-destaque); }
    :host(.chegada) { --cor-cheio: var(--cor-ok); }
    :host(.encerrada) { --cor-cheio: var(--cor-encerrada); }
    .trilha { display: inline-grid; grid-template-columns: repeat(7, 10px); gap: 2px; flex: none; }
    .trilha i { display: block; height: 5px; border-radius: 2px; background: var(--cor-trilho); }
    .trilha i.cheio { background: var(--cor-cheio); }
    .rotulo { font-size: 13px; white-space: nowrap; }
    :host(.encerrada) .rotulo, :host(.interesse) .rotulo { color: var(--cor-texto-suave); }

    :host:has(.completa) { display: block; }
    .completa { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; margin: 0; padding: 0; list-style: none; }
    .completa li { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--cor-texto-suave); min-width: 0; }
    .completa li i { display: block; height: 6px; border-radius: 3px; background: var(--cor-trilho); }
    .completa li.cheio i { background: var(--cor-cheio); }
    .completa li.cheio { color: var(--cor-texto); }
    .completa li.atual { font-weight: 700; }
    .completa li span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
  host: { '[class]': 'categoria()' },
})
export class TrajetoriaComponent {
  readonly status = input.required<Status>();
  /** Para encerradas: última etapa alcançada antes de encerrar (vem do histórico). */
  readonly alcancada = input<Status | null>(null);
  readonly modo = input<'mini' | 'completa'>('mini');
  readonly comRotulo = input(true);

  readonly etapas = ETAPAS_TRAJETORIA;
  readonly rotulosCurtos = ROTULO_ETAPA_CURTO;

  readonly categoria = computed<Categoria>(() => {
    const s = this.status();
    if (s === 'INTERESSE') return 'interesse';
    if (s === 'RECUSADA' || s === 'SEM_RETORNO') return 'encerrada';
    return s === 'PROPOSTA' || s === 'ACEITA' ? 'chegada' : 'andamento';
  });

  /** Quantas etapas foram alcançadas (0 a 7). */
  readonly posicao = computed(() => {
    const etapa = this.categoria() === 'encerrada' ? this.alcancada() : this.status();
    return etapa ? ETAPAS_TRAJETORIA.indexOf(etapa) + 1 : 0;
  });

  readonly rotulo = computed(() => ROTULO_STATUS[this.status()]);

  readonly descricao = computed(() => {
    const p = this.posicao();
    if (this.categoria() === 'interesse') return 'Interesse: ainda não enviada';
    const onde = p ? `após ${ROTULO_STATUS[ETAPAS_TRAJETORIA[p - 1]]}` : '';
    return this.categoria() === 'encerrada'
      ? `${this.rotulo()} ${onde}`.trim()
      : `Etapa ${p} de ${ETAPAS_TRAJETORIA.length}: ${this.rotulo()}`;
  });
}
