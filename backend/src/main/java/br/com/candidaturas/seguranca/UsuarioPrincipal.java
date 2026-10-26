package br.com.candidaturas.seguranca;

/** Usuário autenticado pelo token do agente ou por um job rodando em nome dele. */
public record UsuarioPrincipal(Long id, String email) {
}
