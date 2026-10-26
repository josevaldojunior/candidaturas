import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { SessaoService } from './core/sessao.service';
import { LogoComponent } from './shared/logo.component';
import { SeletorTemaComponent } from './shared/seletor-tema.component';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
}

const MENU: ItemMenu[] = [
  { rota: '/dashboard', rotulo: 'Dashboard', icone: 'insights' },
  { rota: '/kanban', rotulo: 'Quadro', icone: 'view_kanban' },
  { rota: '/candidaturas', rotulo: 'Candidaturas', icone: 'work_outline' },
  { rota: '/follow-ups', rotulo: 'Follow-ups', icone: 'notifications_active' },
  { rota: '/contatos', rotulo: 'Contatos', icone: 'contacts' },
  { rota: '/configuracoes', rotulo: 'Configurações', icone: 'settings' },
];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule, SeletorTemaComponent, LogoComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly menu = MENU;
  readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  /** Na tela de login o app aparece sem o menu lateral e sem a barra superior. */
  readonly telaDeLogin = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects.startsWith('/login')),
    ),
    { initialValue: location.pathname.startsWith('/login') },
  );
  /** No celular o menu lateral vira gaveta (hambúrguer). */
  readonly celular = toSignal(
    inject(BreakpointObserver).observe([Breakpoints.XSmall, Breakpoints.Small]).pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  fecharSeCelular(sidenav: MatSidenav): void {
    if (this.celular()) {
      sidenav.close();
    }
  }
}
