import { HttpClient } from '@angular/common/http';
import { Injectable, effect, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { PlataformaCadastro } from './models';
import { SessaoService } from './sessao.service';
import { Notificacao } from './notificacao.service';

export type PlataformaEdicao = Pick<PlataformaCadastro, 'nome' | 'ordem' | 'ativa'>;
@Injectable({ providedIn: 'root' })
export class PlataformasService {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(SessaoService);
  private readonly notificacao = inject(Notificacao);
  readonly itens = signal<PlataformaCadastro[]>([]);
  constructor() {
    effect((onCleanup) => {
      const id = this.sessao.usuario()?.id;
      this.itens.set([]);
      if (id) {
        const sub = this.carregar().subscribe({ error: e => this.notificacao.erro(e, 'Não foi possível carregar as plataformas') });
        onCleanup(() => sub.unsubscribe());
      }
    }, { allowSignalWrites: true });
  }
  carregar() { return this.http.get<PlataformaCadastro[]>('/api/plataformas').pipe(tap(v => this.itens.set(v))); }
  criar(v: PlataformaEdicao) { return this.http.post<PlataformaCadastro>('/api/plataformas', v); }
  atualizar(id: number, v: PlataformaEdicao) { return this.http.put<PlataformaCadastro>(`/api/plataformas/${id}`, v); }
  excluir(id: number) { return this.http.delete<void>(`/api/plataformas/${id}`); }
  nome(codigo: string) { return this.itens().find(p => p.codigo === codigo)?.nome ?? codigo; }
}
