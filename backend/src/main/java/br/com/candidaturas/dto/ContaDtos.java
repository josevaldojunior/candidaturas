package br.com.candidaturas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

public final class ContaDtos {

    private ContaDtos() {
    }

    public record CadastroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String senha) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, LocalDateTime criadoEm) {
    }

    /** Resposta de login/cadastro: o token vai no header Authorization: Bearer nas próximas chamadas. */
    public record SessaoResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
    }

    public record ConfigPublica(boolean cadastroAberto) {
    }

    public record TokenIntegracaoStatus(boolean configurado, LocalDateTime criadoEm) {
    }

    /** O token só aparece aqui, uma vez; depois só fica o hash no banco. */
    public record TokenIntegracaoGerado(String token, LocalDateTime criadoEm) {
    }

    public record TrocaSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String novaSenha) {
    }
}
