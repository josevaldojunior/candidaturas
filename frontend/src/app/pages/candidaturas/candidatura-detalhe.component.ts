import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { CandidaturaApi, ContatoApi } from '../../core/api.service';
import { ConsultaListaService } from '../../core/consulta-lista.service';
import { paraIsoData } from '../../core/datas';
import { CandidaturaDetalhe, Contato, Interacao, Status, TipoInteracao } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { STATUS, TIPOS_INTERACAO } from '../../core/rotulos';
import { confirmar } from '../../shared/confirmacao.dialog';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-candidatura-detalhe',
  standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink, RotuloPipe, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule, MatDatepickerModule, MatExpansionModule, MatCheckboxModule,
    MatMenuModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './candidatura-detalhe.component.html',
  styleUrl: './candidatura-detalhe.component.scss',
})
export class CandidaturaDetalheComponent implements OnInit {
  private readonly api = inject(CandidaturaApi);
  private readonly contatoApi = inject(ContatoApi);
  private readonly notificacao = inject(Notificacao);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  readonly consulta = inject(ConsultaListaService);

  readonly id = input.required<string>();

  readonly status = STATUS;
  readonly tiposInteracao = TIPOS_INTERACAO;
  readonly c = signal<CandidaturaDetalhe | null>(null);
  readonly todosContatos = signal<Contato[]>([]);
  readonly contatosDisponiveis = computed(() => {
    const vinculados = new Set(this.c()?.contatos.map((ct) => ct.id) ?? []);
    return this.todosContatos().filter((ct) => !vinculados.has(ct.id));
  });
  readonly historicoRecente = computed(() => [...(this.c()?.historico ?? [])].reverse());
  readonly salvandoInteracao = signal(false);

  readonly novaInteracao = inject(FormBuilder).group({
    tipo: ['EMAIL' as TipoInteracao, Validators.required],
    descricao: ['', Validators.maxLength(4000)],
    proximoPasso: ['', Validators.maxLength(1000)],
    dataFollowUp: [null as Date | null],
  });

  ngOnInit(): void {
    this.carregar();
    this.contatoApi.listar().subscribe((l) => this.todosContatos.set(l));
  }

  carregar(): void {
    this.api.buscar(+this.id()).subscribe({
      next: (c) => this.c.set(c),
      error: (e) => {
        this.notificacao.erro(e, 'Candidatura não encontrada');
        this.router.navigate(['/candidaturas'], { queryParams: this.consulta.ultima() });
      },
    });
  }

  mudarStatus(novo: Status): void {
    const atual = this.c();
    if (!atual || novo === atual.status) {
      return;
    }
    this.api.mudarStatus(atual.id, novo).subscribe({
      next: (c) => {
        this.c.set(c);
        this.notificacao.ok('Status atualizado');
      },
      error: (e) => this.notificacao.erro(e),
    });
  }

  adicionarInteracao(): void {
    const atual = this.c();
    if (!atual || this.novaInteracao.invalid) {
      return;
    }
    const v = this.novaInteracao.getRawValue();
    this.salvandoInteracao.set(true);
    this.api.adicionarInteracao(atual.id, {
      tipo: v.tipo!,
      descricao: v.descricao || null,
      proximoPasso: v.proximoPasso || null,
      dataFollowUp: paraIsoData(v.dataFollowUp),
    }).subscribe({
      next: () => {
        this.salvandoInteracao.set(false);
        this.novaInteracao.reset({ tipo: 'EMAIL', descricao: '', proximoPasso: '', dataFollowUp: null });
        this.notificacao.ok('Interação registrada');
        this.carregar();
      },
      error: (e) => {
        this.salvandoInteracao.set(false);
        this.notificacao.erro(e);
      },
    });
  }

  alternarFollowUp(i: Interacao, concluido: boolean): void {
    this.api.concluirFollowUp(i.id, concluido).subscribe({
      next: () => this.carregar(),
      error: (e) => this.notificacao.erro(e),
    });
  }

  excluirInteracao(i: Interacao): void {
    confirmar(this.dialog, { titulo: 'Excluir interação', mensagem: 'Deseja excluir esta interação?', confirmar: 'Excluir' })
      .pipe(filter(Boolean), switchMap(() => this.api.excluirInteracao(i.id)))
      .subscribe({ next: () => this.carregar(), error: (e) => this.notificacao.erro(e) });
  }

  vincular(contatoId: number): void {
    this.api.vincularContato(+this.id(), contatoId).subscribe({
      next: (c) => this.c.set(c),
      error: (e) => this.notificacao.erro(e),
    });
  }

  desvincular(contato: Contato): void {
    this.api.desvincularContato(+this.id(), contato.id).subscribe({
      next: (c) => this.c.set(c),
      error: (e) => this.notificacao.erro(e),
    });
  }

  excluir(): void {
    const atual = this.c();
    if (!atual) {
      return;
    }
    confirmar(this.dialog, {
      titulo: 'Excluir candidatura',
      mensagem: `Excluir a candidatura para ${atual.empresa} (${atual.cargo})? Interações e histórico também serão apagados.`,
      confirmar: 'Excluir',
    })
      .pipe(filter(Boolean), switchMap(() => this.api.excluir(atual.id)))
      .subscribe({
        next: () => {
          this.notificacao.ok('Candidatura excluída');
          this.router.navigate(['/candidaturas'], { queryParams: this.consulta.ultima() });
        },
        error: (e) => this.notificacao.erro(e),
      });
  }
}
