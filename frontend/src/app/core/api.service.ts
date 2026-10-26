import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  CandidaturaDetalhe, CandidaturaRequest, CandidaturaResumo, ColunaKanban, Contato, ContatoRequest, Dashboard,
  FiltrosCandidatura, FollowUps, ResultadoImportacao, ResultadoInbox, ResultadoSemRetorno, Interacao, InteracaoRequest, Pagina, Status,
} from './models';

@Injectable({ providedIn: 'root' })
export class CandidaturaApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/candidaturas';

  listar(filtros: FiltrosCandidatura, page = 0, size = 20, sort = 'atualizadoEm,desc'): Observable<Pagina<CandidaturaResumo>> {
    const params = paramsDeFiltros(filtros).set('page', page).set('size', size).set('sort', sort);
    return this.http.get<Pagina<CandidaturaResumo>>(this.base, { params });
  }

  buscar(id: number): Observable<CandidaturaDetalhe> {
    return this.http.get<CandidaturaDetalhe>(`${this.base}/${id}`);
  }

  criar(req: CandidaturaRequest): Observable<CandidaturaDetalhe> {
    return this.http.post<CandidaturaDetalhe>(this.base, req);
  }

  atualizar(id: number, req: CandidaturaRequest): Observable<CandidaturaDetalhe> {
    return this.http.put<CandidaturaDetalhe>(`${this.base}/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  mudarStatus(id: number, status: Status, observacao?: string): Observable<CandidaturaDetalhe> {
    return this.http.patch<CandidaturaDetalhe>(`${this.base}/${id}/status`, { status, observacao });
  }

  kanban(): Observable<ColunaKanban[]> {
    return this.http.get<ColunaKanban[]>(`${this.base}/kanban`);
  }

  stacks(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/stacks`);
  }

  empresas(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/empresas`);
  }

  adicionarInteracao(id: number, req: InteracaoRequest): Observable<Interacao> {
    return this.http.post<Interacao>(`${this.base}/${id}/interacoes`, req);
  }

  atualizarInteracao(id: number, req: InteracaoRequest): Observable<Interacao> {
    return this.http.put<Interacao>(`/api/interacoes/${id}`, req);
  }

  excluirInteracao(id: number): Observable<void> {
    return this.http.delete<void>(`/api/interacoes/${id}`);
  }

  concluirFollowUp(id: number, concluido = true): Observable<Interacao> {
    return this.http.patch<Interacao>(`/api/interacoes/${id}/follow-up`, null, { params: { concluido } });
  }

  vincularContato(id: number, contatoId: number): Observable<CandidaturaDetalhe> {
    return this.http.post<CandidaturaDetalhe>(`${this.base}/${id}/contatos/${contatoId}`, null);
  }

  desvincularContato(id: number, contatoId: number): Observable<CandidaturaDetalhe> {
    return this.http.delete<CandidaturaDetalhe>(`${this.base}/${id}/contatos/${contatoId}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ContatoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/contatos';

  listar(): Observable<Contato[]> {
    return this.http.get<Contato[]>(this.base);
  }

  criar(req: ContatoRequest): Observable<Contato> {
    return this.http.post<Contato>(this.base, req);
  }

  atualizar(id: number, req: ContatoRequest): Observable<Contato> {
    return this.http.put<Contato>(`${this.base}/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

export function paramsDeFiltros(f: FiltrosCandidatura): HttpParams {
  let p = new HttpParams();
  for (const [chave, valor] of Object.entries(f)) {
    if (valor === null || valor === undefined || valor === '') {
      continue;
    }
    if (Array.isArray(valor)) {
      valor.forEach((v) => (p = p.append(chave, v)));
    } else {
      p = p.set(chave, String(valor));
    }
  }
  return p;
}

@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  resumo(): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/dashboard');
  }
}

@Injectable({ providedIn: 'root' })
export class AcompanhamentoApi {
  private readonly http = inject(HttpClient);

  followUps(): Observable<FollowUps> {
    return this.http.get<FollowUps>('/api/follow-ups');
  }

  diasSemRetorno(): Observable<{ dias: number }> {
    return this.http.get<{ dias: number }>('/api/sem-retorno/configuracao');
  }

  salvarDiasSemRetorno(dias: number): Observable<{ dias: number }> {
    return this.http.put<{ dias: number }>('/api/sem-retorno/configuracao', { dias });
  }

  executarSemRetorno(): Observable<ResultadoSemRetorno> {
    return this.http.post<ResultadoSemRetorno>('/api/sem-retorno/executar', null);
  }
}

@Injectable({ providedIn: 'root' })
export class CsvApi {
  private readonly http = inject(HttpClient);

  /**
   * Baixa o CSV com os mesmos filtros da listagem. É feito pela API (e não por um link) porque o
   * download precisa levar o token de login.
   */
  exportar(filtros: FiltrosCandidatura = {}): Observable<void> {
    return this.http.get('/api/csv/exportar', {
      params: paramsDeFiltros(filtros), responseType: 'blob', observe: 'response',
    }).pipe(map((r) => {
      const nome = /filename="([^"]+)"/.exec(r.headers.get('Content-Disposition') ?? '')?.[1] ?? 'candidaturas.csv';
      const url = URL.createObjectURL(r.body as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = nome;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    }));
  }

  importar(arquivo: File): Observable<ResultadoImportacao> {
    const dados = new FormData();
    dados.append('arquivo', arquivo, arquivo.name);
    return this.http.post<ResultadoImportacao>('/api/csv/importar', dados);
  }

  processarInbox(): Observable<ResultadoInbox> {
    return this.http.post<ResultadoInbox>('/api/inbox/processar', null);
  }
}

export interface TokenIntegracaoStatus {
  configurado: boolean;
  criadoEm: string | null;
}

@Injectable({ providedIn: 'root' })
export class ContaApi {
  private readonly http = inject(HttpClient);

  statusToken(): Observable<TokenIntegracaoStatus> {
    return this.http.get<TokenIntegracaoStatus>('/api/conta/token-integracao');
  }

  /** O token só vem nesta resposta; depois não dá para ver de novo, só gerar outro. */
  gerarToken(): Observable<{ token: string; criadoEm: string }> {
    return this.http.post<{ token: string; criadoEm: string }>('/api/conta/token-integracao', null);
  }

  revogarToken(): Observable<void> {
    return this.http.delete<void>('/api/conta/token-integracao');
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<void> {
    return this.http.put<void>('/api/conta/senha', { senhaAtual, novaSenha });
  }
}
