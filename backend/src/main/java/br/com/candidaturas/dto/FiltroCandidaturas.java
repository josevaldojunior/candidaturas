package br.com.candidaturas.dto;

import br.com.candidaturas.domain.Plataforma;
import br.com.candidaturas.domain.Prioridade;
import br.com.candidaturas.domain.Status;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtros da lista de candidaturas (e da exportação CSV), vindos da query string.
 * Campos nulos ou vazios são ignorados.
 */
public record FiltroCandidaturas(
        @Parameter(description = "Status atual (aceita vários)") List<Status> status,
        @Parameter(description = "Parte do nome da empresa") String empresa,
        @Parameter(description = "Plataforma (aceita várias)") List<Plataforma> plataforma,
        @Parameter(description = "Tag de stack, sem diferenciar maiúsculas") String stack,
        @Parameter(description = "Prioridade (aceita várias)") List<Prioridade> prioridade,
        @Parameter(description = "Data da candidatura a partir de (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
        @Parameter(description = "Data da candidatura até (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
        @Parameter(description = "Busca em empresa, cargo, link e observações") String q,
        @Parameter(description = "true = só candidaturas com pendência") Boolean pendentes,
        @Parameter(description = "Etapa do funil alcançada em algum momento (considera o histórico), ex.: ENTREVISTA_TECNICA")
        Status etapaMinima,
        @Parameter(description = "true = tiveram resposta: chegaram à triagem ou além (histórico) ou foram recusadas")
        Boolean comResposta) {

    public static FiltroCandidaturas vazio() {
        return new FiltroCandidaturas(null, null, null, null, null, null, null, null, null, null, null);
    }
}
