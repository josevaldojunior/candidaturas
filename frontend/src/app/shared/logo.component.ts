import { Component, input } from '@angular/core';

/** Nome do sistema, usado no logo e no título das abas. */
export const NOME_SISTEMA = 'CandiTrack';

/** Logo do CandiTrack (mesmo desenho do favicon), acompanhando o tema claro/escuro. */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <svg viewBox="0 0 32 32" [attr.width]="tamanho()" [attr.height]="tamanho()" aria-hidden="true">
      <rect x="1" y="1" width="30" height="30" rx="8" class="fundo" />
      <g class="traco" fill="none" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="7,21 12.5,15.5 16.5,19 24,11" />
        <polyline points="18.5,11 24,11 24,16.5" />
      </g>
    </svg>
    @if (comNome()) {
      <span class="nome">{{ nome }}</span>
    }
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; }
    svg { flex: none; }
    .fundo { fill: var(--cor-destaque); }
    .traco { stroke: var(--cor-logo-traco); }
    .nome { font-weight: 600; letter-spacing: -0.01em; color: var(--cor-destaque); }
  `,
})
export class LogoComponent {
  readonly tamanho = input(28);
  readonly comNome = input(true);
  readonly nome = NOME_SISTEMA;
}
