import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { mensagemDeErro } from '../../core/notificacao.service';
import { SessaoService } from '../../core/sessao.service';
import { LogoComponent } from '../../shared/logo.component';
import { SeletorTemaComponent } from '../../shared/seletor-tema.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, SeletorTemaComponent, LogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** Para onde voltar depois do login (?voltar=/candidaturas...). */
  readonly voltar = input<string>();

  readonly modo = signal<'entrar' | 'cadastrar'>('entrar');
  readonly cadastroAberto = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly verSenha = signal(false);

  readonly entrar = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  readonly cadastro = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  ngOnInit(): void {
    this.sessao.config().subscribe({ next: (c) => this.cadastroAberto.set(c.cadastroAberto), error: () => {} });
  }

  alternar(modo: 'entrar' | 'cadastrar'): void {
    this.modo.set(modo);
    this.erro.set(null);
  }

  enviar(): void {
    const form = this.modo() === 'entrar' ? this.entrar : this.cadastro;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    const chamada = this.modo() === 'entrar'
      ? this.sessao.login(this.entrar.getRawValue().email, this.entrar.getRawValue().senha)
      : this.sessao.cadastrar(this.cadastro.getRawValue().nome, this.cadastro.getRawValue().email,
        this.cadastro.getRawValue().senha);
    chamada.subscribe({
      next: () => this.router.navigateByUrl(this.destinoSeguro()),
      error: (e) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível entrar'));
      },
    });
  }

  /** Só aceita caminhos internos, para o ?voltar= não virar um redirecionamento para fora do site. */
  private destinoSeguro(): string {
    const v = this.voltar();
    return v && v.startsWith('/') && !v.startsWith('//') && !v.startsWith('/login') ? v : '/dashboard';
  }
}
