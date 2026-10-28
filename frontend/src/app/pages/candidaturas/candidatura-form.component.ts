import { PlataformasService } from '../../core/plataformas.service';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { CandidaturaApi, ContatoApi } from '../../core/api.service';
import { ConsultaListaService } from '../../core/consulta-lista.service';
import { deIsoData, paraIsoData } from '../../core/datas';
import {
  CandidaturaDetalhe, CandidaturaRequest, Contato, Modalidade, Plataforma, Prioridade, Status, TipoContrato,
} from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { MODALIDADES, PRIORIDADES, STATUS, TIPOS_CONTRATO } from '../../core/rotulos';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-candidatura-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, RotuloPipe, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatChipsModule, MatAutocompleteModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './candidatura-form.component.html',
  styleUrl: './candidatura-form.component.scss',
})
export class CandidaturaFormComponent implements OnInit {
  private readonly api = inject(CandidaturaApi);
  private readonly contatoApi = inject(ContatoApi);
  private readonly notificacao = inject(Notificacao);
  private readonly router = inject(Router);
  readonly consulta = inject(ConsultaListaService);

  /** Preenchido pela rota /candidaturas/:id/editar. */
  readonly id = input<string>();

  readonly status = STATUS;
  readonly catalogo = inject(PlataformasService);
  readonly plataformas = this.catalogo.itens;
  readonly modalidades = MODALIDADES;
  readonly tiposContrato = TIPOS_CONTRATO;
  readonly prioridades = PRIORIDADES;
  readonly separadores = [ENTER, COMMA, SEMICOLON] as const;

  readonly stack = signal<string[]>([]);
  readonly stacksConhecidas = signal<string[]>([]);
  readonly textoStack = signal('');
  readonly sugestoesStack = computed(() => {
    const t = this.textoStack().toLowerCase();
    const atuais = new Set(this.stack().map((s) => s.toLowerCase()));
    return this.stacksConhecidas().filter((s) => !atuais.has(s.toLowerCase()) && s.toLowerCase().includes(t)).slice(0, 8);
  });
  readonly contatos = signal<Contato[]>([]);
  readonly salvando = signal(false);
  /** Texto da pendência que será concluída ao salvar (mostrado como aviso no formulário). */
  readonly pendenciaConcluida = signal<string | null>(null);
  private original: CandidaturaDetalhe | null = null;
  private readonly destroyRef = inject(DestroyRef);
  readonly carregando = signal(false);

  readonly form = inject(FormBuilder).group({
    empresa: ['', [Validators.required, Validators.maxLength(200)]],
    cargo: ['', [Validators.required, Validators.maxLength(200)]],
    linkVaga: ['', Validators.maxLength(1000)],
    plataforma: [null as Plataforma | null],
    modalidade: ['REMOTO' as Modalidade | null],
    tipoContrato: [null as TipoContrato | null],
    faixaSalarial: ['', Validators.maxLength(100)],
    pretensaoSalarial: ['', Validators.maxLength(100)],
    nota: [null as number | null, [Validators.min(1), Validators.max(10)]],
    dataCandidatura: [new Date() as Date | null],
    status: ['CANDIDATADO' as Status],
    prioridade: ['MEDIA' as Prioridade],
    curriculoVersao: ['', Validators.maxLength(100)],
    observacoes: ['', Validators.maxLength(4000)],
    pendencia: ['', Validators.maxLength(1000)],
    chaveExterna: ['', Validators.maxLength(300)],
    contatoIds: [[] as number[]],
  });

  get edicao(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    this.concluirPendenciaAoSairDeInteresse();
    this.api.stacks().subscribe((s) => this.stacksConhecidas.set(s));
    this.contatoApi.listar().subscribe((c) => this.contatos.set(c));
    const id = this.id();
    if (id) {
      this.carregando.set(true);
      this.api.buscar(+id).subscribe({
        next: (c) => this.preencher(c),
        error: (e) => {
          this.notificacao.erro(e, 'Candidatura não encontrada');
          this.router.navigate(['/candidaturas'], { queryParams: this.consulta.ultima() });
        },
      });
    }
  }

  adicionarStack(e: MatChipInputEvent): void {
    this.incluirTags(e.value);
    e.chipInput.clear();
    this.textoStack.set('');
  }

  selecionarStack(e: MatAutocompleteSelectedEvent, campo: HTMLInputElement): void {
    this.incluirTags(e.option.viewValue);
    campo.value = '';
    this.textoStack.set('');
  }

  removerStack(tag: string): void {
    this.stack.update((l) => l.filter((t) => t !== tag));
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: CandidaturaRequest = {
      empresa: v.empresa!.trim(),
      cargo: v.cargo!.trim(),
      linkVaga: v.linkVaga || null,
      plataforma: v.plataforma,
      modalidade: v.modalidade,
      tipoContrato: v.tipoContrato,
      faixaSalarial: v.faixaSalarial || null,
      pretensaoSalarial: v.pretensaoSalarial || null,
      nota: v.nota,
      stack: this.stack(),
      dataCandidatura: paraIsoData(v.dataCandidatura),
      status: v.status,
      prioridade: v.prioridade,
      curriculoVersao: v.curriculoVersao || null,
      observacoes: v.observacoes || null,
      pendencia: v.pendencia?.trim() || null,
      chaveExterna: v.chaveExterna || null,
      contatoIds: v.contatoIds ?? [],
    };
    this.salvando.set(true);
    const id = this.id();
    const chamada = id ? this.api.atualizar(+id, req) : this.api.criar(req);
    chamada.subscribe({
      next: (c) => {
        this.notificacao.ok(id ? 'Candidatura atualizada' : 'Candidatura criada');
        this.router.navigate(['/candidaturas', c.id]);
      },
      error: (e) => {
        this.salvando.set(false);
        this.notificacao.erro(e, 'Erro ao salvar');
      },
    });
  }

  private incluirTags(texto: string): void {
    const novas = (texto ?? '').split(/[;,]/).map((t) => t.trim()).filter((t) => t.length > 0 && t.length <= 60);
    this.stack.update((atual) => {
      const resultado = [...atual];
      for (const n of novas) {
        if (!resultado.some((t) => t.toLowerCase() === n.toLowerCase())) {
          resultado.push(n);
        }
      }
      return resultado;
    });
  }

  /**
   * Mesma regra do backend: sair de "Interesse" conclui a pendência. O campo é apagado na hora
   * (e restaurado se voltar para Interesse); um texto novo digitado depois é mantido.
   */
  private concluirPendenciaAoSairDeInteresse(): void {
    this.form.controls.status.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((novo) => {
      const original = this.original;
      if (!original || original.status !== 'INTERESSE' || !original.pendencia) {
        return;
      }
      const campo = this.form.controls.pendencia;
      if (novo !== 'INTERESSE' && campo.value === original.pendencia) {
        campo.setValue('');
        this.pendenciaConcluida.set(original.pendencia);
      } else if (novo === 'INTERESSE' && !campo.value) {
        campo.setValue(original.pendencia);
        this.pendenciaConcluida.set(null);
      }
    });
  }

  private preencher(c: CandidaturaDetalhe): void {
    this.original = c;
    this.form.patchValue({
      empresa: c.empresa,
      cargo: c.cargo,
      linkVaga: c.linkVaga ?? '',
      plataforma: c.plataforma,
      modalidade: c.modalidade,
      tipoContrato: c.tipoContrato,
      faixaSalarial: c.faixaSalarial ?? '',
      pretensaoSalarial: c.pretensaoSalarial ?? '',
      nota: c.nota,
      dataCandidatura: deIsoData(c.dataCandidatura),
      status: c.status,
      prioridade: c.prioridade,
      curriculoVersao: c.curriculoVersao ?? '',
      observacoes: c.observacoes ?? '',
      pendencia: c.pendencia ?? '',
      chaveExterna: c.chaveExterna ?? '',
      contatoIds: c.contatos.map((ct) => ct.id),
    });
    this.stack.set([...c.stack]);
    this.carregando.set(false);
  }
}
