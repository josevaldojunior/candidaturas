package br.com.candidaturas.web;

import br.com.candidaturas.domain.Plataforma;
import br.com.candidaturas.domain.Prioridade;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.dto.CandidaturaDtos.*;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoRequest;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoResponse;
import br.com.candidaturas.dto.Pagina;
import br.com.candidaturas.service.CandidaturaService;
import br.com.candidaturas.service.InteracaoService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
@Tag(name = "Candidaturas")
public class CandidaturaController {

    private final CandidaturaService service;
    private final InteracaoService interacaoService;

    public CandidaturaController(CandidaturaService service, InteracaoService interacaoService) {
        this.service = service;
        this.interacaoService = interacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista candidaturas com filtros e paginação",
            description = "Filtros opcionais; status, plataforma e prioridade aceitam vários valores. "
                    + "'q' busca em empresa, cargo, link e observações.")
    public Pagina<CandidaturaResumo> listar(
            @RequestParam(required = false) List<Status> status,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) List<Plataforma> plataforma,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) List<Prioridade> prioridade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Parameter(description = "true = só candidaturas com pendência") Boolean pendentes,
            @ParameterObject @PageableDefault(size = 20, sort = "atualizadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.listar(status, empresa, plataforma, stack, prioridade, de, ate, q, pendentes, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da candidatura com contatos, interações e histórico de status")
    public CandidaturaDetalhe buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma candidatura (origem MANUAL)")
    public CandidaturaDetalhe criar(@Valid @RequestBody CandidaturaRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma candidatura; mudança de status grava histórico")
    public CandidaturaDetalhe atualizar(@PathVariable Long id, @Valid @RequestBody CandidaturaRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Muda o status (grava histórico e interação MUDANCA_STATUS)")
    public CandidaturaDetalhe mudarStatus(@PathVariable Long id, @Valid @RequestBody MudancaStatusRequest req) {
        return service.mudarStatus(id, req);
    }

    @PostMapping("/{id}/interacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public InteracaoResponse adicionarInteracao(@PathVariable Long id, @Valid @RequestBody InteracaoRequest req) {
        return interacaoService.adicionar(id, req);
    }

    @PostMapping("/{id}/contatos/{contatoId}")
    public CandidaturaDetalhe vincularContato(@PathVariable Long id, @PathVariable Long contatoId) {
        return service.vincularContato(id, contatoId);
    }

    @DeleteMapping("/{id}/contatos/{contatoId}")
    public CandidaturaDetalhe desvincularContato(@PathVariable Long id, @PathVariable Long contatoId) {
        return service.desvincularContato(id, contatoId);
    }

    @GetMapping("/kanban")
    @Operation(summary = "Candidaturas agrupadas por status, na ordem do pipeline")
    public List<ColunaKanban> kanban() {
        return service.kanban();
    }

    @GetMapping("/stacks")
    @Operation(summary = "Tags de stack já usadas (para autocompletar)")
    public List<String> stacks() {
        return service.stacks();
    }

    @GetMapping("/empresas")
    @Operation(summary = "Empresas já cadastradas (para autocompletar)")
    public List<String> empresas() {
        return service.empresas();
    }
}
