import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, map } from 'rxjs';
import { CandidaturaApi } from '../../core/api.service';
import { paraIsoData } from '../../core/datas';
import { CandidaturaResumo, FiltrosCandidatura, Plataforma, Prioridade, Status } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { PLATAFORMAS, PRIORIDADES, STATUS } from '../../core/rotulos';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-candidatura-lista',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, RotuloPipe, MatTableModule, MatPaginatorModule, MatSortModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule, MatButtonModule, MatIconModule,
    MatExpansionModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './candidatura-lista.component.html',
  styleUrl: './candidatura-lista.component.scss',
})
export class CandidaturaListaComponent implements OnInit {
  private readonly api = inject(CandidaturaApi);
  private readonly notificacao = inject(Notificacao);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly status = STATUS;
  readonly plataformas = PLATAFORMAS;
  readonly prioridades = PRIORIDADES;
  readonly colunas = ['empresa', 'status', 'plataforma', 'stack', 'prioridade', 'dataCandidatura', 'ultimaInteracaoEm'];

  readonly celular = toSignal(
    inject(BreakpointObserver).observe(Breakpoints.XSmall).pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  readonly itens = signal<CandidaturaResumo[]>([]);
  readonly total = signal(0);
  readonly carregando = signal(false);
  readonly stacks = signal<string[]>([]);

  pagina = 0;
  tamanho = 20;
  ordenacao = 'atualizadoEm,desc';

  readonly filtros = inject(FormBuilder).group({
    q: [''],
    status: [[] as Status[]],
    empresa: [''],
    plataforma: [[] as Plataforma[]],
    stack: [''],
    prioridade: [[] as Prioridade[]],
    de: [null as Date | null],
    ate: [null as Date | null],
  });

  ngOnInit(): void {
    this.filtros.valueChanges.pipe(debounceTime(350), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.pagina = 0;
      this.carregar();
    });
    this.api.stacks().subscribe((s) => this.stacks.set(s));
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.api.listar(this.filtrosAtuais(), this.pagina, this.tamanho, this.ordenacao).subscribe({
      next: (p) => {
        this.itens.set(p.content);
        this.total.set(p.totalElements);
        this.carregando.set(false);
      },
      error: (e) => {
        this.carregando.set(false);
        this.notificacao.erro(e, 'Erro ao carregar candidaturas');
      },
    });
  }

  mudarPagina(e: PageEvent): void {
    this.pagina = e.pageIndex;
    this.tamanho = e.pageSize;
    this.carregar();
  }

  ordenar(s: Sort): void {
    this.ordenacao = s.direction ? `${s.active},${s.direction}` : 'atualizadoEm,desc';
    this.carregar();
  }

  limpar(): void {
    this.filtros.reset({ q: '', status: [], empresa: '', plataforma: [], stack: '', prioridade: [], de: null, ate: null });
  }

  abrir(c: CandidaturaResumo): void {
    this.router.navigate(['/candidaturas', c.id]);
  }

  filtrosAtuais(): FiltrosCandidatura {
    const v = this.filtros.getRawValue();
    return {
      q: v.q ?? undefined,
      status: v.status ?? [],
      empresa: v.empresa ?? undefined,
      plataforma: v.plataforma ?? [],
      stack: v.stack ?? undefined,
      prioridade: v.prioridade ?? [],
      de: paraIsoData(v.de) ?? undefined,
      ate: paraIsoData(v.ate) ?? undefined,
    };
  }
}
