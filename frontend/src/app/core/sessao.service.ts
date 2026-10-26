import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  criadoEm: string;
}

interface Sessao {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

const CHAVE = 'sessao';

/**
 * Sessão do usuário: guarda o JWT devolvido pelo login/cadastro no localStorage
 * e expõe quem está logado. O interceptor usa o token em cada chamada à API.
 */
@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessao = signal<Sessao | null>(this.lerSalva());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly logado = computed(() => this.sessao() !== null);

  get token(): string | null {
    const s = this.sessao();
    if (!s) {
      return null;
    }
    if (new Date(s.expiraEm).getTime() <= Date.now()) {
      this.limpar();
      return null;
    }
    return s.token;
  }

  config(): Observable<{ cadastroAberto: boolean }> {
    return this.http.get<{ cadastroAberto: boolean }>('/api/auth/config');
  }

  login(email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>('/api/auth/login', { email, senha }).pipe(tap((s) => this.guardar(s)));
  }

  cadastrar(nome: string, email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>('/api/auth/cadastro', { nome, email, senha }).pipe(tap((s) => this.guardar(s)));
  }

  /** Sai e volta para o login, lembrando para onde voltar depois. */
  sair(voltarPara?: string): void {
    this.limpar();
    this.router.navigate(['/login'], { queryParams: voltarPara ? { voltar: voltarPara } : {} });
  }

  private guardar(s: Sessao): void {
    this.sessao.set(s);
    try {
      localStorage.setItem(CHAVE, JSON.stringify(s));
    } catch {
      // armazenamento indisponível: a sessão vale só enquanto a aba estiver aberta
    }
  }

  private limpar(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE);
    } catch {
      // ignora
    }
  }

  private lerSalva(): Sessao | null {
    try {
      const bruto = localStorage.getItem(CHAVE);
      if (!bruto) {
        return null;
      }
      const s = JSON.parse(bruto) as Sessao;
      return new Date(s.expiraEm).getTime() > Date.now() ? s : null;
    } catch {
      return null;
    }
  }
}
