package br.com.candidaturas.web;
import br.com.candidaturas.dto.PlataformaDtos.*;
import br.com.candidaturas.service.PlataformaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/plataformas") @Tag(name="Plataformas")
public class PlataformaController {
    private final PlataformaService service;
    public PlataformaController(PlataformaService service) { this.service=service; }
    @GetMapping public List<Response> listar() { return service.listar(); }
    @GetMapping("/{id}") public Response buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Response criar(@Valid @RequestBody Request r) { return service.criar(r); }
    @PutMapping("/{id}") public Response atualizar(@PathVariable Long id,@Valid @RequestBody Request r) { return service.atualizar(id,r); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
