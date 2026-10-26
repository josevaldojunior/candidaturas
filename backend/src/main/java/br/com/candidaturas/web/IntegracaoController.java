package br.com.candidaturas.web;

import br.com.candidaturas.domain.Origem;
import br.com.candidaturas.dto.IntegracaoDtos.ExisteResponse;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoItem;
import br.com.candidaturas.service.IntegracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Entrada para agentes e automações. Autenticação pelo header X-Integracao-Token (gerado em
 * Configurações, um por conta) ou por login; os dados vão para a conta dona do token.
 */
@RestController
@RequestMapping("/api/integracao/candidaturas")
@Tag(name = "Integração (agentes e automações)")
@SecurityRequirement(name = "tokenIntegracao")
@SecurityRequirement(name = "login")
public class IntegracaoController {

    private final IntegracaoService service;

    public IntegracaoController(IntegracaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Upsert em lote (origem AGENTE) na conta dona do token")
    public ResultadoImportacao importar(@RequestBody List<IntegracaoItem> itens) {
        return service.processar(itens, Origem.AGENTE);
    }

    @GetMapping("/existe")
    @Operation(summary = "Verifica se a vaga já foi registrada na conta (por link/chave ou empresa+cargo)")
    public ExisteResponse existe(@RequestParam(required = false) String link,
                                 @RequestParam(required = false) String empresa,
                                 @RequestParam(required = false) String cargo) {
        return service.existe(link, empresa, cargo);
    }
}
