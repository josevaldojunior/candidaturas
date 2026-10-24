import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ModoTema = 'auto' | 'light' | 'dark';

/** Mesma chave do portfólio, então a escolha é lembrada entre visitas. */
const CHAVE = 'tema';

/**
 * Tema claro/escuro: "auto" segue o sistema (sem data-theme no html);
 * "light" e "dark" forçam o modo e ficam salvos no localStorage.
 * O index.html aplica o valor salvo antes do Angular carregar, para não piscar.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly raiz = inject(DOCUMENT).documentElement;

  readonly modo = signal<ModoTema>(this.lerModo());

  aplicar(modo: ModoTema): void {
    if (modo === 'auto') {
      this.raiz.removeAttribute('data-theme');
    } else {
      this.raiz.setAttribute('data-theme', modo);
    }
    try {
      if (modo === 'auto') {
        localStorage.removeItem(CHAVE);
      } else {
        localStorage.setItem(CHAVE, modo);
      }
    } catch {
      // armazenamento indisponível (modo privado): o tema vale só nesta visita
    }
    this.modo.set(modo);
  }

  private lerModo(): ModoTema {
    const atual = this.raiz.getAttribute('data-theme');
    return atual === 'light' || atual === 'dark' ? atual : 'auto';
  }
}
