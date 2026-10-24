package br.com.candidaturas.web;

import br.com.candidaturas.dto.FollowUpDtos.FollowUpsResponse;
import br.com.candidaturas.service.FollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follow-ups")
@Tag(name = "Follow-ups")
public class FollowUpController {

    private final FollowUpService service;

    public FollowUpController(FollowUpService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Follow-ups pendentes: vencidos e dos próximos 7 dias",
            description = "Para concluir, use PATCH /api/interacoes/{id}/follow-up.")
    public FollowUpsResponse pendentes() {
        return service.pendentes();
    }
}
