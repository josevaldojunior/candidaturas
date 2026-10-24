import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ErroApi } from './models';

@Injectable({ providedIn: 'root' })
export class Notificacao {
  private readonly snack = inject(MatSnackBar);

  ok(mensagem: string): void {
    this.snack.open(mensagem, 'OK', { duration: 3000 });
  }

  erro(e: unknown, padrao = 'Não foi possível concluir a operação'): void {
    this.snack.open(mensagemDeErro(e, padrao), 'Fechar', { duration: 6000, panelClass: 'snack-erro' });
  }
}

export function mensagemDeErro(e: unknown, padrao: string): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'API indisponível. Verifique se o backend está rodando em http://localhost:8080.';
    }
    const corpo = e.error as Partial<ErroApi> | null;
    if (corpo?.campos?.length) {
      return `${corpo.mensagem}: ${corpo.campos.map((c) => `${c.campo} ${c.mensagem}`).join('; ')}`;
    }
    if (corpo?.mensagem) {
      return corpo.mensagem;
    }
  }
  return padrao;
}
