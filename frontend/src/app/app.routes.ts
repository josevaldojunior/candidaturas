import { Routes } from '@angular/router';
import { somenteDeslogado, somenteLogado } from './core/autenticacao';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar',
    canActivate: [somenteDeslogado],
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    title: 'Dashboard',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'candidaturas',
    title: 'Candidaturas',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/candidaturas/candidatura-lista.component').then((m) => m.CandidaturaListaComponent),
  },
  {
    path: 'candidaturas/nova',
    title: 'Nova candidatura',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/candidaturas/candidatura-form.component').then((m) => m.CandidaturaFormComponent),
  },
  {
    path: 'candidaturas/:id/editar',
    title: 'Editar candidatura',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/candidaturas/candidatura-form.component').then((m) => m.CandidaturaFormComponent),
  },
  {
    path: 'candidaturas/:id',
    title: 'Candidatura',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/candidaturas/candidatura-detalhe.component').then((m) => m.CandidaturaDetalheComponent),
  },
  {
    path: 'kanban',
    title: 'Quadro',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/kanban/kanban.component').then((m) => m.KanbanComponent),
  },
  {
    path: 'follow-ups',
    title: 'Follow-ups',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/follow-ups/follow-ups.component').then((m) => m.FollowUpsComponent),
  },
  {
    path: 'configuracoes',
    title: 'Configurações',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/configuracoes/configuracoes.component').then((m) => m.ConfiguracoesComponent),
  },
  {
    path: 'contatos',
    title: 'Contatos',
    canActivate: [somenteLogado],
    loadComponent: () => import('./pages/contatos/contatos.component').then((m) => m.ContatosComponent),
  },
  // a tela padrão é o Dashboard, inclusive para endereços desconhecidos
  { path: '**', redirectTo: 'dashboard' },
];
