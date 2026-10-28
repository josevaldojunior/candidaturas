import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Clipboard } from '@angular/cdk/clipboard';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { AcompanhamentoApi, ContaApi, CsvApi, TokenIntegracaoStatus } from '../../core/api.service';
import { confirmar } from '../../shared/confirmacao.dialog';
import { ResultadoImportacao, ResultadoInbox, ResultadoSemRetorno } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [RouterLink, DatePipe, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './configuracoes.component.html',
  styleUrl: './configuracoes.component.scss',
})
export class ConfiguracoesComponent implements OnInit {
  private readonly api = inject(AcompanhamentoApi);
  private readonly csvApi = inject(CsvApi);
  private readonly notificacao = inject(Notificacao);
  private readonly contaApi = inject(ContaApi);
  private readonly clipboard = inject(Clipboard);
  private readonly dialog = inject(MatDialog);

  readonly statusToken = signal<TokenIntegracaoStatus | null>(null);
  /** Token recém-gerado: só existe nesta tela até sair dela. */
  readonly tokenGerado = signal<string | null>(null);
  readonly origem = location.origin;
  /** Chaves do JSON no exemplo de curl (o template do Angular não aceita { } soltos). */
  readonly abre = '{';
  readonly fecha = '}';

  readonly senha = inject(FormBuilder).nonNullable.group({
    atual: ['', Validators.required],
    nova: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  readonly dias = new FormControl<number>(30, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(1), Validators.max(365)],
  });
  readonly ultimaExecucao = signal<ResultadoSemRetorno | null>(null);
  readonly executando = signal(false);
  readonly importando = signal(false);
  readonly resultadoImportacao = signal<ResultadoImportacao | null>(null);
  readonly resultadoInbox = signal<ResultadoInbox | null>(null);

  ngOnInit(): void {
    this.contaApi.statusToken().subscribe({ next: (s) => this.statusToken.set(s), error: (e) => this.notificacao.erro(e) });
    this.api.diasSemRetorno().subscribe({
      next: (c) => this.dias.setValue(c.dias),
      error: (e) => this.notificacao.erro(e),
    });
  }

  salvarDias(): void {
    if (this.dias.invalid) {
      this.dias.markAsTouched();
      return;
    }
    this.api.salvarDiasSemRetorno(this.dias.value).subscribe({
      next: (c) => {
        this.dias.setValue(c.dias);
        this.dias.markAsPristine();
        this.notificacao.ok(`Prazo salvo: ${c.dias} dias`);
      },
      error: (e) => this.notificacao.erro(e),
    });
  }

  executarSemRetorno(): void {
    this.executando.set(true);
    this.api.executarSemRetorno().subscribe({
      next: (r) => {
        this.executando.set(false);
        this.ultimaExecucao.set(r);
        this.notificacao.ok(r.marcadas ? `${r.marcadas} candidatura(s) marcadas como sem retorno` : 'Nenhuma candidatura parada');
      },
      error: (e) => {
        this.executando.set(false);
        this.notificacao.erro(e);
      },
    });
  }

  gerarToken(): void {
    const gerar = () => this.contaApi.gerarToken().subscribe({
      next: (t) => {
        this.tokenGerado.set(t.token);
        this.statusToken.set({ configurado: true, criadoEm: t.criadoEm });
      },
      error: (e) => this.notificacao.erro(e),
    });
    if (!this.statusToken()?.configurado) {
      gerar();
      return;
    }
    confirmar(this.dialog, {
      titulo: 'Gerar novo token',
      mensagem: 'O token atual vai parar de funcionar. Depois atualize o agente com o novo token.',
      confirmar: 'Gerar novo',
    }).pipe(filter(Boolean)).subscribe(() => gerar());
  }

  copiarToken(): void {
    const t = this.tokenGerado();
    if (t && this.clipboard.copy(t)) {
      this.notificacao.ok('Token copiado');
    }
  }

  revogarToken(): void {
    confirmar(this.dialog, {
      titulo: 'Revogar token',
      mensagem: 'O agente deixa de conseguir enviar candidaturas para a sua conta até você gerar outro token.',
      confirmar: 'Revogar',
    }).pipe(filter(Boolean), switchMap(() => this.contaApi.revogarToken())).subscribe({
      next: () => {
        this.tokenGerado.set(null);
        this.statusToken.set({ configurado: false, criadoEm: null });
        this.notificacao.ok('Token revogado');
      },
      error: (e) => this.notificacao.erro(e),
    });
  }

  trocarSenha(): void {
    if (this.senha.invalid) {
      this.senha.markAllAsTouched();
      return;
    }
    const v = this.senha.getRawValue();
    this.contaApi.trocarSenha(v.atual, v.nova).subscribe({
      next: () => {
        this.senha.reset();
        this.notificacao.ok('Senha alterada');
      },
      error: (e) => this.notificacao.erro(e),
    });
  }

  exportar(): void {
    this.csvApi.exportar().subscribe({ error: (e) => this.notificacao.erro(e, 'Erro ao exportar') });
  }

  importar(campo: HTMLInputElement): void {
    const arquivo = campo.files?.[0];
    if (!arquivo) {
      return;
    }
    this.importando.set(true);
    this.csvApi.importar(arquivo).subscribe({
      next: (r) => {
        this.importando.set(false);
        this.resultadoImportacao.set(r);
        campo.value = '';
        this.notificacao.ok(`${r.criadas} criada(s), ${r.atualizadas} atualizada(s)`);
      },
      error: (e) => {
        this.importando.set(false);
        campo.value = '';
        this.notificacao.erro(e, 'Erro ao importar o CSV');
      },
    });
  }

  processarInbox(): void {
    this.csvApi.processarInbox().subscribe({
      next: (r) => {
        this.resultadoInbox.set(r);
        this.notificacao.ok(r.arquivos.length ? `${r.arquivos.length} arquivo(s) processado(s)` : 'Nenhum CSV na pasta inbox');
      },
      error: (e) => this.notificacao.erro(e),
    });
  }
}
