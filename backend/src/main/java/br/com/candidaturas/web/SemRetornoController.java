package br.com.candidaturas.web;

import br.com.candidaturas.dto.SemRetornoDtos.ConfiguracaoSemRetorno;
import br.com.candidaturas.dto.SemRetornoDtos.ResultadoSemRetorno;
import br.com.candidaturas.service.SemRetornoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sem-retorno")
@Tag(name = "Sem retorno")
public class SemRetornoController {

    private final SemRetornoService service;

    public SemRetornoController(SemRetornoService service) {
        this.service = service;
    }

    @GetMapping("/configuracao")
    public ConfiguracaoSemRetorno configuracao() {
        return new ConfiguracaoSemRetorno(service.dias());
    }

    @PutMapping("/configuracao")
    @Operation(summary = "Define após quantos dias sem interação a candidatura vira SEM_RETORNO")
    public ConfiguracaoSemRetorno salvar(@Valid @RequestBody ConfiguracaoSemRetorno req) {
        return new ConfiguracaoSemRetorno(service.salvarDias(req.dias()));
    }

    @PostMapping("/executar")
    @Operation(summary = "Executa agora a marcação automática (o job também roda todo dia)")
    public ResultadoSemRetorno executar() {
        return service.executar();
    }
}
