package br.com.candidaturas.web;

import br.com.candidaturas.service.NaoAutorizadoException;
import br.com.candidaturas.service.RecursoNaoEncontradoException;
import br.com.candidaturas.service.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Converte exceções em um corpo de erro padronizado ({@link ErroResposta}). */
@RestControllerAdvice
public class ErroHandler {

    private static final Logger log = LoggerFactory.getLogger(ErroHandler.class);

    private final Clock clock;

    public ErroHandler(Clock clock) {
        this.clock = clock;
    }

    public record CampoErro(String campo, String mensagem) {
    }

    public record ErroResposta(OffsetDateTime timestamp, int status, String erro, String mensagem, String caminho,
                               List<CampoErro> campos) {
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    ResponseEntity<ErroResposta> naoAutorizado(NaoAutorizadoException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage(), req, List.of());
    }

    /**
     * Corpo JSON inválido (@Valid) e filtros da query string que não convertem (ex.: status=XYZ).
     * MethodArgumentNotValidException é uma BindException, então este handler cobre os dois casos.
     */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ErroResposta> validacao(BindException e, HttpServletRequest req) {
        List<CampoErro> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new CampoErro(f.getField(), f.isBindingFailure()
                        ? "valor inválido: '" + f.getRejectedValue() + "'" : f.getDefaultMessage()))
                .toList();
        var conversao = e.getBindingResult().getFieldErrors().stream().filter(FieldError::isBindingFailure).findFirst();
        String mensagem = conversao
                .map(f -> "Valor inválido para o parâmetro '" + f.getField() + "': " + f.getRejectedValue())
                .orElse("Dados inválidos");
        return resposta(HttpStatus.BAD_REQUEST, mensagem, req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroResposta> violacao(ConstraintViolationException e, HttpServletRequest req) {
        List<CampoErro> campos = e.getConstraintViolations().stream()
                .map(v -> new CampoErro(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", req, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, IllegalArgumentException.class})
    ResponseEntity<ErroResposta> parametroInvalido(Exception e, HttpServletRequest req) {
        String msg = e instanceof MethodArgumentTypeMismatchException m
                ? "Valor inválido para o parâmetro '" + m.getName() + "': " + m.getValue()
                : e.getMessage();
        return resposta(HttpStatus.BAD_REQUEST, msg, req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Operação viola uma restrição do banco (registro duplicado?)", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResposta> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResposta> rotaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResposta> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", req, List.of());
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                                  List<CampoErro> campos) {
        return ResponseEntity.status(status).body(new ErroResposta(OffsetDateTime.now(clock), status.value(),
                status.getReasonPhrase(), mensagem, req.getRequestURI(), campos));
    }
}
