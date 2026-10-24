import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AcompanhamentoApi, CsvApi } from '../../core/api.service';
import { ResultadoImportacao, ResultadoInbox, ResultadoSemRetorno } from '../../core/models';
import { Notificacao } from '../../core/notificacao.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './configuracoes.component.html',
  styleUrl: './configuracoes.component.scss',
})
export class ConfiguracoesComponent implements OnInit {
  private readonly api = inject(AcompanhamentoApi);
  private readonly csvApi = inject(CsvApi);
  private readonly notificacao = inject(Notificacao);

  readonly dias = new FormControl<number>(30, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(1), Validators.max(365)],
  });
  readonly ultimaExecucao = signal<ResultadoSemRetorno | null>(null);
  readonly executando = signal(false);
  readonly importando = signal(false);
  readonly resultadoImportacao = signal<ResultadoImportacao | null>(null);
  readonly resultadoInbox = signal<ResultadoInbox | null>(null);
  readonly urlExportacao = this.csvApi.urlExportacao();

  ngOnInit(): void {
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
