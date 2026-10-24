import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AcompanhamentoApi } from '../../core/api.service';
import { ResultadoSemRetorno } from '../../core/models';
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
  private readonly notificacao = inject(Notificacao);

  readonly dias = new FormControl<number>(21, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(1), Validators.max(365)],
  });
  readonly ultimaExecucao = signal<ResultadoSemRetorno | null>(null);
  readonly executando = signal(false);

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
}
