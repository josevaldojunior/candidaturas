import { Component, inject } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ModoTema, TemaService } from '../core/tema.service';

interface OpcaoTema {
  modo: ModoTema;
  titulo: string;
  rotulo: string;
}

/** Seletor de tema em três posições (automático, claro, escuro), no mesmo estilo do portfólio. */
@Component({
  selector: 'app-seletor-tema',
  standalone: true,
  imports: [MatTooltipModule],
  template: `
    <div class="seletor" role="group" aria-label="Tema">
      @for (o of opcoes; track o.modo) {
        <button type="button" [attr.aria-pressed]="tema.modo() === o.modo" [attr.aria-label]="o.rotulo"
                [matTooltip]="o.titulo" (click)="tema.aplicar(o.modo)">
          @switch (o.modo) {
            @case ('auto') {
              <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></svg>
            }
            @case ('light') {
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
            }
            @case ('dark') {
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
            }
          }
        </button>
      }
    </div>
  `,
  styles: `
    .seletor {
      display: inline-flex; gap: 2px; padding: 3px; border: 1px solid var(--cor-borda); border-radius: 999px;
      background: var(--cor-superficie); transition: background-color .25s, border-color .25s;
    }
    button {
      display: grid; place-items: center; width: 30px; height: 30px; padding: 0; border: 0; border-radius: 999px;
      background: transparent; color: var(--cor-texto-suave); cursor: pointer;
    }
    button:hover { color: var(--cor-texto); }
    button:focus-visible { outline: 2px solid var(--cor-destaque); outline-offset: 1px; }
    button[aria-pressed='true'] { background: var(--cor-destaque-suave); color: var(--cor-destaque); }
    svg { width: 16px; height: 16px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  `,
})
export class SeletorTemaComponent {
  readonly tema = inject(TemaService);

  readonly opcoes: OpcaoTema[] = [
    { modo: 'auto', titulo: 'Automático (sistema)', rotulo: 'Tema automático, segue o sistema' },
    { modo: 'light', titulo: 'Claro', rotulo: 'Tema claro' },
    { modo: 'dark', titulo: 'Escuro', rotulo: 'Tema escuro' },
  ];
}
