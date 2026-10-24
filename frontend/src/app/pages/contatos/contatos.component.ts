import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { filter, switchMap } from 'rxjs';
import { ContatoApi } from '../../core/api.service';
import { CargoContato, Contato, ContatoRequest } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';
import { CARGOS_CONTATO } from '../../core/rotulos';
import { confirmar } from '../../shared/confirmacao.dialog';
import { RotuloPipe } from '../../shared/rotulo.pipe';

@Component({
  selector: 'app-contatos',
  standalone: true,
  imports: [RotuloPipe, MatButtonModule, MatIconModule],
  template: `
    <div class="pagina">
      <div class="pagina-cabecalho">
        <h1>Contatos</h1>
        <button mat-flat-button (click)="editar()"><mat-icon>person_add</mat-icon> Novo</button>
      </div>
      <div class="grade">
        @for (c of contatos(); track c.id) {
          <div class="cartao contato">
            <div class="topo">
              <div>
                <strong>{{ c.nome }}</strong>
                <div class="texto-suave">{{ c.cargo | rotulo: 'cargoContato' }}{{ c.empresa ? ' · ' + c.empresa : '' }}</div>
              </div>
              <div>
                <button mat-icon-button aria-label="Editar" (click)="editar(c)"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button aria-label="Excluir" (click)="excluir(c)"><mat-icon>delete_outline</mat-icon></button>
              </div>
            </div>
            @if (c.email) { <div><mat-icon inline>mail</mat-icon> <a [href]="'mailto:' + c.email">{{ c.email }}</a></div> }
            @if (c.telefone) { <div><mat-icon inline>phone</mat-icon> {{ c.telefone }}</div> }
            @if (c.linkedin) { <div><mat-icon inline>link</mat-icon> <a [href]="c.linkedin" target="_blank" rel="noopener">LinkedIn</a></div> }
          </div>
        } @empty {
          <div class="cartao vazio">Nenhum contato cadastrado.</div>
        }
      </div>
    </div>
  `,
  styles: `
    .grade { display: grid; gap: 12px; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); }
    .contato { display: grid; gap: 4px; word-break: break-word; }
    .topo { display: flex; justify-content: space-between; align-items: flex-start; }
  `,
})
export class ContatosComponent implements OnInit {
  private readonly api = inject(ContatoApi);
  private readonly dialog = inject(MatDialog);
  private readonly notificacao = inject(Notificacao);

  readonly contatos = signal<Contato[]>([]);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.listar().subscribe({ next: (l) => this.contatos.set(l), error: (e) => this.notificacao.erro(e) });
  }

  editar(contato?: Contato): void {
    this.dialog.open(ContatoDialog, { data: contato ?? null, width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.carregar());
  }

  excluir(c: Contato): void {
    confirmar(this.dialog, { titulo: 'Excluir contato', mensagem: `Excluir ${c.nome}?`, confirmar: 'Excluir' })
      .pipe(filter(Boolean), switchMap(() => this.api.excluir(c.id)))
      .subscribe({
        next: () => {
          this.notificacao.ok('Contato excluído');
          this.carregar();
        },
        error: (e) => this.notificacao.erro(e),
      });
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RotuloPipe, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ dados ? 'Editar contato' : 'Novo contato' }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="grade-form">
        <mat-form-field class="largura-total">
          <mat-label>Nome</mat-label>
          <input matInput formControlName="nome" required>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Cargo</mat-label>
          <mat-select formControlName="cargo">
            @for (c of cargos; track c) { <mat-option [value]="c">{{ c | rotulo: 'cargoContato' }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Empresa</mat-label>
          <input matInput formControlName="empresa">
        </mat-form-field>
        <mat-form-field>
          <mat-label>E-mail</mat-label>
          <input matInput formControlName="email" type="email">
          @if (form.controls.email.invalid) { <mat-error>E-mail inválido</mat-error> }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Telefone</mat-label>
          <input matInput formControlName="telefone">
        </mat-form-field>
        <mat-form-field class="largura-total">
          <mat-label>LinkedIn</mat-label>
          <input matInput formControlName="linkedin">
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ContatoDialog {
  private readonly api = inject(ContatoApi);
  private readonly notificacao = inject(Notificacao);
  private readonly ref = inject(MatDialogRef<ContatoDialog>);
  readonly dados = inject<Contato | null>(MAT_DIALOG_DATA);
  readonly cargos = CARGOS_CONTATO;

  readonly form = inject(FormBuilder).group({
    nome: [this.dados?.nome ?? '', [Validators.required, Validators.maxLength(200)]],
    cargo: [(this.dados?.cargo ?? 'RECRUTADOR') as CargoContato],
    email: [this.dados?.email ?? '', [Validators.email, Validators.maxLength(200)]],
    telefone: [this.dados?.telefone ?? '', Validators.maxLength(50)],
    linkedin: [this.dados?.linkedin ?? '', Validators.maxLength(500)],
    empresa: [this.dados?.empresa ?? '', Validators.maxLength(200)],
  });

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: ContatoRequest = {
      nome: v.nome!.trim(),
      cargo: v.cargo!,
      email: v.email || null,
      telefone: v.telefone || null,
      linkedin: v.linkedin || null,
      empresa: v.empresa || null,
    };
    const chamada = this.dados ? this.api.atualizar(this.dados.id, req) : this.api.criar(req);
    chamada.subscribe({
      next: () => {
        this.notificacao.ok('Contato salvo');
        this.ref.close(true);
      },
      error: (e) => this.notificacao.erro(e),
    });
  }
}
