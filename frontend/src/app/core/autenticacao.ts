import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessaoService } from './sessao.service';

/** Manda o JWT nas chamadas à API; se a API responder 401, a sessão acabou e volta para o login. */
export const autenticacaoInterceptor: HttpInterceptorFn = (req, next) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const rotaDeLogin = req.url.startsWith('/api/auth/');
  const token = sessao.token;
  const requisicao = token && req.url.startsWith('/api/') && !rotaDeLogin
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  return next(requisicao).pipe(
    catchError((e: unknown) => {
      if (e instanceof HttpErrorResponse && e.status === 401 && !rotaDeLogin) {
        sessao.sair(router.url);
      }
      return throwError(() => e);
    }),
  );
};

/** Rotas que exigem login: sem sessão, vai para /login e volta depois. */
export const somenteLogado: CanActivateFn = (_rota, estado) => {
  const sessao = inject(SessaoService);
  if (sessao.token) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { voltar: estado.url } });
};

/** A tela de login não faz sentido para quem já entrou. */
export const somenteDeslogado: CanActivateFn = () => {
  const sessao = inject(SessaoService);
  return sessao.token ? inject(Router).createUrlTree(['/dashboard']) : true;
};
