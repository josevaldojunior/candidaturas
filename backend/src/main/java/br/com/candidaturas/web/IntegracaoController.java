package br.com.candidaturas.web;

import br.com.candidaturas.domain.Origem;
import br.com.candidaturas.dto.IntegracaoDtos.ExisteResponse;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoItem;
import br.com.candidaturas.service.IntegracaoService;
import br.com.candidaturas.service.NaoAutorizadoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/integracao/candidaturas")
@Tag(name = "Integração com o agente")
public class IntegracaoController {

    private final IntegracaoService service;
    private final String token;

    public IntegracaoController(IntegracaoService service, @Value("${integracao.token:}") String token) {
        this.service = service;
        this.token = token == null ? "" : token.trim();
    }

    @PostMapping
    @Operation(summary = "Upsert em lote vindo do agente (origem AGENTE)")
    public ResultadoImportacao importar(@RequestHeader(value = "X-Integracao-Token", required = false) String recebido,
                                        @RequestBody List<IntegracaoItem> itens) {
        validarToken(recebido);
        return service.processar(itens, Origem.AGENTE);
    }

    @GetMapping("/existe")
    @Operation(summary = "Verifica se a vaga já foi registrada (por link/chave ou empresa+cargo)")
    public ExisteResponse existe(@RequestHeader(value = "X-Integracao-Token", required = false) String recebido,
                                 @RequestParam(required = false) String link,
                                 @RequestParam(required = false) String empresa,
                                 @RequestParam(required = false) String cargo) {
        validarToken(recebido);
        return service.existe(link, empresa, cargo);
    }

    /** Só exige o header quando INTEGRACAO_TOKEN estiver definido. */
    private void validarToken(String recebido) {
        if (token.isEmpty()) {
            return;
        }
        byte[] esperado = token.getBytes(StandardCharsets.UTF_8);
        byte[] informado = (recebido == null ? "" : recebido).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, informado)) {
            throw new NaoAutorizadoException("Token de integração ausente ou inválido (header X-Integracao-Token)");
        }
    }
}
