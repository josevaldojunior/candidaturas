import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { PlataformasService } from '../../core/plataformas.service';
import { PlataformaCadastro } from '../../core/models';
import { Notificacao, mensagemDeErro } from '../../core/notificacao.service';
import { confirmar } from '../../shared/confirmacao.dialog';

@Component({
  standalone: true,
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
  <div class="pagina">
    <div class="pagina-cabecalho">
      <a mat-icon-button routerLink="/configuracoes" aria-label="Voltar para configurações"><mat-icon>arrow_back</mat-icon></a>
      <h1>Plataformas</h1>
      <button mat-flat-button (click)="editar()"><mat-icon>add</mat-icon> Nova plataforma</button>
    </div>
    <p class="texto-suave">Organize onde você encontra suas vagas. Plataformas desativadas continuam no histórico e nos filtros.</p>
    <div class="cartao">
      @for (p of api.itens(); track p.id) {
        <div class="item">
          <span class="ordem" aria-label="Ordem">{{ p.ordem }}</span>
          <div class="nome"><strong>{{ p.nome }}</strong><div class="texto-suave">{{ p.ativa ? 'Ativa' : 'Desativada' }}</div></div>
          <button mat-icon-button (click)="editar(p)" [attr.aria-label]="'Editar ' + p.nome"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button (click)="excluir(p)" [disabled]="p.codigo === 'OUTRO'" [attr.aria-label]="'Excluir ' + p.nome"><mat-icon>delete_outline</mat-icon></button>
        </div>
      } @empty { <div class="vazio">Nenhuma plataforma carregada.</div> }
    </div>
  </div>`,
  styles: `.item { display:flex; gap:12px; align-items:center; padding:12px 0; border-bottom:1px solid var(--cor-borda); }
    .item:last-child { border:0; } .nome { flex:1; min-width:0; overflow-wrap:anywhere; } .ordem { color:var(--cor-texto-suave); min-width:28px; }
    .nome .texto-suave { font-size:13px; }`
})
export class PlataformasComponent {
  readonly api = inject(PlataformasService);
  private readonly dialog = inject(MatDialog);
  private readonly notificacao = inject(Notificacao);
  editar(p?: PlataformaCadastro) {
    this.dialog.open(PlataformaDialog, { data: { p, ordem: this.api.itens().length }, width:'480px', maxWidth:'95vw' })
      .afterClosed().pipe(filter(Boolean), switchMap(() => this.api.carregar())).subscribe({ error:e=>this.notificacao.erro(e) });
  }
  excluir(p: PlataformaCadastro) {
    confirmar(this.dialog, { titulo:'Excluir plataforma', mensagem:`Excluir “${p.nome}”? Se ela tiver candidaturas, desative-a em Editar.`, confirmar:'Excluir' })
      .pipe(filter(Boolean), switchMap(()=>this.api.excluir(p.id)), switchMap(()=>this.api.carregar()))
      .subscribe({ next:()=>this.notificacao.ok('Plataforma excluída'), error:e=>this.notificacao.erro(e) });
  }
}

@Component({
  standalone:true,
  imports:[ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSlideToggleModule],
  template:`<h2 mat-dialog-title>{{ dados.p ? 'Editar plataforma' : 'Nova plataforma' }}</h2>
  <form [formGroup]="form" (ngSubmit)="salvar()">
    <mat-dialog-content>
      <div class="grade-form">
        <mat-form-field class="largura-total"><mat-label>Nome</mat-label><input matInput formControlName="nome" maxlength="120">
          @if(form.controls.nome.invalid) { <mat-error>Informe o nome da plataforma</mat-error> }</mat-form-field>
        <mat-form-field><mat-label>Ordem na lista</mat-label><input matInput type="number" formControlName="ordem" min="0" max="9999">
          <mat-hint>Menores números aparecem primeiro</mat-hint>
          @if(form.controls.ordem.invalid) { <mat-error>Use um inteiro entre 0 e 9999</mat-error> }</mat-form-field>
        <mat-slide-toggle formControlName="ativa">Ativa</mat-slide-toggle>
      </div>
      @if(dados.p?.codigo === 'OUTRO') { <p class="texto-suave">Outros deve permanecer ativa para receber origens não reconhecidas.</p> }
      @if(erro()) { <p role="alert" style="color:var(--cor-perigo)">{{ erro() }}</p> }
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button type="submit" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Salvar' }}</button></mat-dialog-actions>
  </form>`
})
class PlataformaDialog {
  readonly dados = inject<{p?:PlataformaCadastro; ordem:number}>(MAT_DIALOG_DATA);
  private readonly api = inject(PlataformasService);
  private readonly ref = inject(MatDialogRef<PlataformaDialog>);
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    nome:[this.dados.p?.nome ?? '', [Validators.required, Validators.pattern(/.*\S.*/), Validators.maxLength(120)]],
    ordem:[this.dados.p?.ordem ?? this.dados.ordem,[Validators.required, Validators.min(0),Validators.max(9999), Validators.pattern(/^\d+$/)]],
    ativa:[{value:this.dados.p?.ativa ?? true, disabled:this.dados.p?.codigo === 'OUTRO'}],
  });
  salvar() {
    if(this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.salvando.set(true); this.erro.set('');
    const v=this.form.getRawValue();
    (this.dados.p ? this.api.atualizar(this.dados.p.id,v) : this.api.criar(v)).subscribe({
      next:()=>this.ref.close(true), error:e=>{this.salvando.set(false);this.erro.set(mensagemDeErro(e,'Não foi possível salvar'));}
    });
  }
}
