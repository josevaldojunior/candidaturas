package br.com.candidaturas.seguranca;

import br.com.candidaturas.web.ErroHandler.ErroResposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** 401/403 da camada de segurança no mesmo corpo padronizado dos outros erros da API. */
@Component
public class ErroSeguranca {

    private final ObjectMapper mapper;
    private final Clock clock;

    public ErroSeguranca(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public AuthenticationEntryPoint naoAutenticado() {
        return (req, resp, e) -> escrever(req, resp, HttpStatus.UNAUTHORIZED,
                "Faça login (header Authorization: Bearer <token>) ou envie o X-Integracao-Token do agente");
    }

    public AccessDeniedHandler semPermissao() {
        return (req, resp, e) -> escrever(req, resp, HttpStatus.FORBIDDEN, "Acesso negado");
    }

    void escrever(HttpServletRequest req, HttpServletResponse resp, HttpStatus status, String mensagem)
            throws IOException {
        resp.setStatus(status.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getOutputStream(), new ErroResposta(OffsetDateTime.now(clock), status.value(),
                status.getReasonPhrase(), mensagem, req.getRequestURI(), List.of()));
    }
}
