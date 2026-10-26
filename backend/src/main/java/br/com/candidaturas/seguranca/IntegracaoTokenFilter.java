package br.com.candidaturas.seguranca;

import br.com.candidaturas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica o agente pelo header X-Integracao-Token nas rotas /api/integracao/**.
 * O token identifica a conta: os dados enviados vão para o usuário dono do token.
 */
public class IntegracaoTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Integracao-Token";

    private final UsuarioRepository usuarioRepository;
    private final ErroSeguranca erros;

    public IntegracaoTokenFilter(UsuarioRepository usuarioRepository, ErroSeguranca erros) {
        this.usuarioRepository = usuarioRepository;
        this.erros = erros;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/integracao/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response); // pode estar logado por JWT; senão o entry point responde 401
            return;
        }
        var usuario = usuarioRepository.findByTokenIntegracaoHash(TokenIntegracao.hash(token.trim()));
        if (usuario.isEmpty()) {
            erros.escrever(request, response, HttpStatus.UNAUTHORIZED, "Token de integração inválido ou revogado");
            return;
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsuarioAtual.autenticacao(usuario.get().getId(), usuario.get().getEmail()));
        SecurityContextHolder.setContext(contexto);
        chain.doFilter(request, response);
    }
}
