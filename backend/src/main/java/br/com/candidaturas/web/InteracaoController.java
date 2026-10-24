package br.com.candidaturas.web;

import br.com.candidaturas.dto.InteracaoDtos.InteracaoRequest;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoResponse;
import br.com.candidaturas.service.InteracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interacoes")
@Tag(name = "Interações")
public class InteracaoController {

    private final InteracaoService service;

    public InteracaoController(InteracaoService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public InteracaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody InteracaoRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PatchMapping("/{id}/follow-up")
    @Operation(summary = "Marca (ou desmarca, com concluido=false) o follow-up como concluído")
    public InteracaoResponse concluirFollowUp(@PathVariable Long id,
                                              @RequestParam(defaultValue = "true") boolean concluido) {
        return service.concluirFollowUp(id, concluido);
    }
}
