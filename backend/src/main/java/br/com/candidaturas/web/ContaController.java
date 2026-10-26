package br.com.candidaturas.web;

import br.com.candidaturas.dto.ContaDtos.*;
import br.com.candidaturas.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Conta")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @GetMapping("/api/auth/config")
    @Operation(summary = "Configuração pública da tela de login (se o cadastro está aberto)")
    public ConfigPublica config() {
        return service.config();
    }

    @PostMapping("/api/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta e já devolve o token de sessão",
            description = "A primeira conta do sistema assume os registros que existiam antes das contas.")
    public SessaoResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return service.cadastrar(req);
    }

    @PostMapping("/api/auth/login")
    @Operation(summary = "Login com e-mail e senha; use o token no header Authorization: Bearer <token>")
    public SessaoResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @GetMapping("/api/conta")
    public UsuarioResponse eu() {
        return service.eu();
    }

    @PutMapping("/api/conta/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocaSenhaRequest req) {
        service.trocarSenha(req);
    }

    @GetMapping("/api/conta/token-integracao")
    @Operation(summary = "Diz se a conta tem token de integração (o valor nunca é mostrado de novo)")
    public TokenIntegracaoStatus statusToken() {
        return service.statusToken();
    }

    @PostMapping("/api/conta/token-integracao")
    @Operation(summary = "Gera um novo token para o agente (invalida o anterior); o valor só aparece nesta resposta")
    public TokenIntegracaoGerado gerarToken() {
        return service.gerarToken();
    }

    @DeleteMapping("/api/conta/token-integracao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga o token do agente")
    public void revogarToken() {
        service.revogarToken();
    }
}
