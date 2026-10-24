import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
}

const MENU: ItemMenu[] = [
  { rota: '/candidaturas', rotulo: 'Candidaturas', icone: 'work_outline' },
  { rota: '/kanban', rotulo: 'Quadro', icone: 'view_kanban' },
  { rota: '/contatos', rotulo: 'Contatos', icone: 'contacts' },
];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, MatButtonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly menu = MENU;
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
