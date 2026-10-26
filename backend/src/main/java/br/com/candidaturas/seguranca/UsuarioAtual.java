package br.com.candidaturas.seguranca;

import br.com.candidaturas.service.NaoAutorizadoException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Identifica o dono dos dados da requisição: login (JWT, subject = id do usuário) ou token do agente.
 * Jobs agendados usam {@link #como} para rodar em nome de cada usuário.
 */
@Component
public class UsuarioAtual {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Long.valueOf(jwt.getToken().getSubject());
        }
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return p.id();
        }
        throw new NaoAutorizadoException("Usuário não autenticado");
    }

    /** Executa a ação autenticado como o usuário informado (usado por jobs, fora de uma requisição). */
    public <T> T como(Long usuarioId, String email, Supplier<T> acao) {
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao(usuarioId, email));
        SecurityContextHolder.setContext(contexto);
        try {
            return acao.get();
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    static Authentication autenticacao(Long usuarioId, String email) {
        return UsernamePasswordAuthenticationToken.authenticated(new UsuarioPrincipal(usuarioId, email), null, List.of());
    }
}
