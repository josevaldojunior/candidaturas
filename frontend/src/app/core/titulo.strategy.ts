import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { NOME_SISTEMA } from '../shared/logo.component';

/** Título da aba do navegador: "Dashboard · CandiTrack" (ou só "CandiTrack" sem título na rota). */
@Injectable()
export class TituloStrategy extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const pagina = this.buildTitle(snapshot);
    this.title.setTitle(pagina ? `${pagina} · ${NOME_SISTEMA}` : NOME_SISTEMA);
  }
}
