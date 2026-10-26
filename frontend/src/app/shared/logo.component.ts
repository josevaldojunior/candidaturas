import { Component, input } from '@angular/core';

/** Nome do sistema, usado no logo e no título das abas. */
export const NOME_SISTEMA = 'CandiTrack';

/** Desenho do foguete (Material Icons "rocket_launch", Apache 2.0), o mesmo do favicon. */
const FOGUETE =
  'M9.19 6.35c-2.04 2.29-3.44 5.58-3.57 5.89L2 10.69l4.05-4.05c.47-.47 1.15-.68 1.81-.55l1.33.26zM11.17 17s3.74-1.55 5.89-3.7c5.4-5.4 4.5-9.62 4.21-10.57-.95-.3-5.17-1.19-10.57 4.21C8.55 9.09 7 12.83 7 12.83L11.17 17zm6.48-2.19c-2.29 2.04-5.58 3.44-5.89 3.57L13.31 22l4.05-4.05c.47-.47.68-1.15.55-1.81l-.26-1.33zM9 18c0 .83-.34 1.58-.88 2.12C6.94 21.3 2 22 2 22s.7-4.94 1.88-6.12C4.42 15.34 5.17 15 6 15c1.66 0 3 1.34 3 3zm4-9c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z';

/** Logo do CandiTrack (mesmo desenho do favicon), acompanhando o tema claro/escuro. */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <svg viewBox="0 0 32 32" [attr.width]="tamanho()" [attr.height]="tamanho()" aria-hidden="true">
      <rect x="1" y="1" width="30" height="30" rx="8" class="fundo" />
      <path class="foguete" transform="translate(4 4)" [attr.d]="foguete" />
    </svg>
    @if (comNome()) {
      <span class="nome">{{ nome }}</span>
    }
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; }
    svg { flex: none; }
    .fundo { fill: var(--cor-destaque); }
    .foguete { fill: var(--cor-logo-traco); }
    .nome { font-family: var(--fonte-titulo); font-weight: 700; letter-spacing: -0.02em; color: var(--cor-destaque); }
  `,
})
export class LogoComponent {
  readonly tamanho = input(28);
  readonly comNome = input(true);
  readonly nome = NOME_SISTEMA;
  readonly foguete = FOGUETE;
}
