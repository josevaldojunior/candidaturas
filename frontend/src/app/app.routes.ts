import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'candidaturas' },
  {
    path: 'candidaturas',
    title: 'Candidaturas',
    loadComponent: () => import('./pages/candidaturas/candidatura-lista.component').then((m) => m.CandidaturaListaComponent),
  },
  {
    path: 'candidaturas/nova',
    title: 'Nova candidatura',
    loadComponent: () => import('./pages/candidaturas/candidatura-form.component').then((m) => m.CandidaturaFormComponent),
  },
  {
    path: 'candidaturas/:id/editar',
    title: 'Editar candidatura',
    loadComponent: () => import('./pages/candidaturas/candidatura-form.component').then((m) => m.CandidaturaFormComponent),
  },
  {
    path: 'candidaturas/:id',
    title: 'Candidatura',
    loadComponent: () => import('./pages/candidaturas/candidatura-detalhe.component').then((m) => m.CandidaturaDetalheComponent),
  },
  {
    path: 'contatos',
    title: 'Contatos',
    loadComponent: () => import('./pages/contatos/contatos.component').then((m) => m.ContatosComponent),
  },
  { path: '**', redirectTo: 'candidaturas' },
];
