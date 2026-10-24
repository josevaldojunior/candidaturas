package br.com.candidaturas.web;

import br.com.candidaturas.dto.DashboardDtos.DashboardResponse;
import br.com.candidaturas.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Indicadores: total, taxa de resposta, funil por etapa, por plataforma e por semana",
            description = "Taxa de resposta = candidaturas enviadas que chegaram pelo menos a TRIAGEM_RH ou foram "
                    + "RECUSADAS, dividido pelas enviadas (tudo que passou de INTERESSE). O funil conta quantas "
                    + "candidaturas alcançaram cada etapa, considerando o histórico de status.")
    public DashboardResponse resumo() {
        return service.resumo();
    }
}
