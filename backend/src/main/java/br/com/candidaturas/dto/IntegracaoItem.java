package br.com.candidaturas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Item enviado pelo agente (JSON) ou lido do CSV. Tudo chega como texto e é normalizado
 * item a item, para que um valor ruim derrube só aquele item e não o lote inteiro.
 */
@Schema(description = "Candidatura enviada pelo agente. Enums aceitam texto livre (ex.: \"Híbrido\", \"Triagem RH\").")
public record IntegracaoItem(
        @Schema(description = "Identificador da vaga no agente; se vazio, o linkVaga é usado como chave") String chaveExterna,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ACME") String empresa,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Desenvolvedor Java Pleno") String cargo,
        @Schema(example = "https://www.linkedin.com/jobs/view/123") String linkVaga,
        @Schema(example = "LinkedIn Vagas", description = "Gupy, LinkedIn Vagas, LinkedIn Feed, Indeed, Programathor, Remotar, InfoJobs, Coodesh, GeekHunter, Catho, Repositórios GitHub; o resto vira OUTRO") String plataforma,
        @Schema(example = "Remoto") String modalidade,
        @Schema(example = "PJ") String tipoContrato,
        String faixaSalarial,
        String pretensaoSalarial,
        @Schema(example = "8") String nota,
        @Schema(example = "2026-10-24", description = "yyyy-MM-dd (o CSV também aceita dd/MM/yyyy)") String dataCandidatura,
        @Schema(example = "Candidatado") String status,
        @Schema(example = "Alta") String prioridade,
        String curriculoVersao,
        String observacoes,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> stack,
        String contatoNome,
        String contatoEmail,
        String contatoTelefone,
        @Schema(example = "e-mail") String tipoInteracao,
        String descricaoInteracao,
        String proximoPasso,
        @Schema(example = "2026-10-31") String dataFollowUp) {
}
