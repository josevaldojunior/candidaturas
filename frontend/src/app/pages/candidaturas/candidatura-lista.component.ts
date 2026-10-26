import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSortModule, Sort, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, ParamMap, Params, Router, RouterLink } from '@angular/router';
import { debounceTime, map } from 'rxjs';
import { CandidaturaApi, CsvApi } from '../../core/api.service';
import { ConsultaListaService } from '../../core/consulta-lista.service';
import { deIsoData, paraIsoData } from '../../core/datas';
import { CandidaturaResumo, FiltrosCandidatura, Plataforma, Prioridade, Status } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { PLATAFORMAS, PRIORIDADES, ROTULO_STATUS, STATUS } from '../../core/rotulos';
import { RotuloPipe } from '../../shared/rotulo.pipe';

const TAMANHO_PADRAO = 20;
const ORDEM_PADRAO = 'atualizadoEm,desc';

@Component({
  selector: 'app-candidatura-lista',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, RotuloPipe, MatTableModule, MatPaginatorModule, MatSortModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule, MatButtonModule, MatIconModule,
    MatExpansionModule, MatProgressBarModule, MatTooltipModule, MatSlideToggleModule],
  templateUrl: './candidatura-lista.component.html',
  styleUrl: './candidatura-lista.component.scss',
})
export class CandidaturaListaComponent implements OnInit {
  private readonly api = inject(CandidaturaApi);
  private readonly notificacao = inject(Notificacao);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly csvApi = inject(CsvApi);
  private readonly consulta = inject(ConsultaListaService);

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

  /** Filtros que só chegam pelos cards do dashboard (sem campo no formulário): aparecem como etiqueta. */
  readonly etapaMinima = signal<Status | null>(null);
  readonly comResposta = signal(false);
  readonly filtrosDoDashboard = computed(() => {
    const lista: { chave: 'etapaMinima' | 'comResposta'; rotulo: string }[] = [];
    const etapa = this.etapaMinima();
    if (etapa) {
      lista.push({
        chave: 'etapaMinima',
        rotulo: etapa === 'ENTREVISTA_TECNICA' ? 'Chegaram à entrevista' : `Chegaram a "${ROTULO_STATUS[etapa]}" ou além`,
      });
    }
    if (this.comResposta()) {
      lista.push({ chave: 'comResposta', rotulo: 'Tiveram resposta' });
    }
    return lista;
  });

  pagina = 0;
  tamanho = TAMANHO_PADRAO;
  ordenacao = ORDEM_PADRAO;


  readonly filtros = inject(FormBuilder).group({
    q: [''],
    status: [[] as Status[]],
    empresa: [''],
    plataforma: [[] as Plataforma[]],
    stack: [''],
    prioridade: [[] as Prioridade[]],
    de: [null as Date | null],
    ate: [null as Date | null],
    pendentes: [false],
  });

  ngOnInit(): void {
    // filtros, página e ordenação vêm da URL: voltar do detalhe (ou o card de pendências do
    // dashboard, que abre ?pendentes=true) reabre a lista exatamente como estava
    this.lerDaUrl(this.route.snapshot.queryParamMap);
    this.filtros.valueChanges.pipe(debounceTime(350), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.pagina = 0;
      this.atualizar();
    });
    this.api.stacks().subscribe((s) => this.stacks.set(s));
    this.atualizar();
  }

  /** Recarrega a lista e grava o estado atual na URL (sem criar entrada nova no histórico). */
  private atualizar(): void {
    const params = this.paramsDaConsulta();
    this.consulta.ultima.set(params);
    this.router.navigate([], { relativeTo: this.route, queryParams: params, replaceUrl: true });
    this.carregar();
  }

  private lerDaUrl(q: ParamMap): void {
    this.filtros.patchValue({
      q: q.get('q') ?? '',
      status: q.getAll('status') as Status[],
      empresa: q.get('empresa') ?? '',
      plataforma: q.getAll('plataforma') as Plataforma[],
      stack: q.get('stack') ?? '',
      prioridade: q.getAll('prioridade') as Prioridade[],
      de: deIsoData(q.get('de')),
      ate: deIsoData(q.get('ate')),
      pendentes: q.get('pendentes') === 'true',
    }, { emitEvent: false });
    this.etapaMinima.set((q.get('etapaMinima') as Status | null) || null);
    this.comResposta.set(q.get('comResposta') === 'true');
    this.pagina = Math.max(0, Number(q.get('pagina')) || 0);
    this.tamanho = Number(q.get('tamanho')) || TAMANHO_PADRAO;
    this.ordenacao = q.get('ordem') || ORDEM_PADRAO;
  }

  /** Só entra na URL o que difere do padrão, para ela ficar curta. */
  private paramsDaConsulta(): Params {
    const f = this.filtrosAtuais();
    const p: Params = {};
    const texto = (v: string | undefined) => (v && v.trim() ? v.trim() : undefined);
    p['q'] = texto(f.q);
    p['empresa'] = texto(f.empresa);
    p['stack'] = texto(f.stack);
    p['status'] = f.status?.length ? f.status : undefined;
    p['plataforma'] = f.plataforma?.length ? f.plataforma : undefined;
    p['prioridade'] = f.prioridade?.length ? f.prioridade : undefined;
    p['de'] = f.de;
    p['ate'] = f.ate;
    p['pendentes'] = f.pendentes ? 'true' : undefined;
    p['etapaMinima'] = f.etapaMinima;
    p['comResposta'] = f.comResposta ? 'true' : undefined;
    p['pagina'] = this.pagina > 0 ? this.pagina : undefined;
    p['tamanho'] = this.tamanho !== TAMANHO_PADRAO ? this.tamanho : undefined;
    p['ordem'] = this.ordenacao !== ORDEM_PADRAO ? this.ordenacao : undefined;
    return Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined));
  }

  get ordemCampo(): string {
    return this.ordenacao.split(',')[0];
  }

  get ordemDirecao(): SortDirection {
    return (this.ordenacao.split(',')[1] as SortDirection) || 'desc';
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
    this.atualizar();
  }

  ordenar(s: Sort): void {
    this.ordenacao = s.direction ? `${s.active},${s.direction}` : ORDEM_PADRAO;
    this.pagina = 0;
    this.atualizar();
  }

  limpar(): void {
    this.etapaMinima.set(null);
    this.comResposta.set(false);
    this.filtros.reset({ q: '', status: [], empresa: '', plataforma: [], stack: '', prioridade: [], de: null, ate: null, pendentes: false });
  }

  removerFiltroDoDashboard(chave: 'etapaMinima' | 'comResposta'): void {
    if (chave === 'etapaMinima') {
      this.etapaMinima.set(null);
    } else {
      this.comResposta.set(false);
    }
    this.pagina = 0;
    this.atualizar();
  }

  exportar(): void {
    this.csvApi.exportar(this.filtrosAtuais()).subscribe({ error: (e) => this.notificacao.erro(e, 'Erro ao exportar') });
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
      pendentes: v.pendentes || undefined,
      etapaMinima: this.etapaMinima() ?? undefined,
      comResposta: this.comResposta() || undefined,
    };
  }
}
