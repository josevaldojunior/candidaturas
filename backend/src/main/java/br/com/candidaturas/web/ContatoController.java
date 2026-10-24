package br.com.candidaturas.web;

import br.com.candidaturas.dto.ContatoDtos.ContatoRequest;
import br.com.candidaturas.dto.ContatoDtos.ContatoResponse;
import br.com.candidaturas.service.ContatoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contatos")
@Tag(name = "Contatos")
public class ContatoController {

    private final ContatoService service;

    public ContatoController(ContatoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContatoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ContatoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContatoResponse criar(@Valid @RequestBody ContatoRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public ContatoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContatoRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
