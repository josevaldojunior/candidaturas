package br.com.candidaturas.dto;

import br.com.candidaturas.domain.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** DTOs de candidatura (records), separados das entidades. */
public final class CandidaturaDtos {

    private CandidaturaDtos() {
    }

    public record CandidaturaRequest(
            @NotBlank @Size(max = 200) String empresa,
            @NotBlank @Size(max = 200) String cargo,
            @Size(max = 1000) String linkVaga,
            Plataforma plataforma,
            Modalidade modalidade,
            TipoContrato tipoContrato,
            @Size(max = 100) String faixaSalarial,
            @Size(max = 100) String pretensaoSalarial,
            @Min(1) @Max(10) Integer nota,
            Set<@NotBlank @Size(max = 60) String> stack,
            LocalDate dataCandidatura,
            Status status,
            Prioridade prioridade,
            @Size(max = 100) String curriculoVersao,
            @Size(max = 4000) String observacoes,
            @Size(max = 300) String chaveExterna,
            List<Long> contatoIds) {
    }

    public record CandidaturaResumo(
            Long id, String empresa, String cargo, String linkVaga, Plataforma plataforma, Modalidade modalidade,
            TipoContrato tipoContrato, String faixaSalarial, String pretensaoSalarial, Integer nota, List<String> stack,
            LocalDate dataCandidatura, Status status, Prioridade prioridade, Origem origem, String chaveExterna,
            LocalDateTime ultimaInteracaoEm, LocalDateTime atualizadoEm) {
    }

    public record CandidaturaDetalhe(
            Long id, String empresa, String cargo, String linkVaga, Plataforma plataforma, Modalidade modalidade,
            TipoContrato tipoContrato, String faixaSalarial, String pretensaoSalarial, Integer nota, List<String> stack,
            LocalDate dataCandidatura, Status status, Prioridade prioridade, String curriculoVersao, String observacoes,
            Origem origem, String chaveExterna, LocalDateTime ultimaInteracaoEm, LocalDateTime criadoEm,
            LocalDateTime atualizadoEm, List<ContatoDtos.ContatoResponse> contatos,
            List<InteracaoDtos.InteracaoResponse> interacoes, List<HistoricoResponse> historico) {
    }

    public record HistoricoResponse(Long id, Status statusAnterior, Status statusNovo, LocalDateTime data) {
    }

    public record MudancaStatusRequest(@NotNull Status status, @Size(max = 4000) String observacao) {
    }

    public record ColunaKanban(Status status, List<CandidaturaResumo> candidaturas) {
    }

    public static CandidaturaResumo resumo(Candidatura c) {
        return new CandidaturaResumo(c.getId(), c.getEmpresa(), c.getCargo(), c.getLinkVaga(), c.getPlataforma(),
                c.getModalidade(), c.getTipoContrato(), c.getFaixaSalarial(), c.getPretensaoSalarial(), c.getNota(),
                c.getStack().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), c.getDataCandidatura(), c.getStatus(), c.getPrioridade(), c.getOrigem(),
                c.getChaveExterna(), c.getUltimaInteracaoEm(), c.getAtualizadoEm());
    }

    public static CandidaturaDetalhe detalhe(Candidatura c) {
        return new CandidaturaDetalhe(c.getId(), c.getEmpresa(), c.getCargo(), c.getLinkVaga(), c.getPlataforma(),
                c.getModalidade(), c.getTipoContrato(), c.getFaixaSalarial(), c.getPretensaoSalarial(), c.getNota(),
                c.getStack().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), c.getDataCandidatura(), c.getStatus(), c.getPrioridade(),
                c.getCurriculoVersao(), c.getObservacoes(), c.getOrigem(), c.getChaveExterna(),
                c.getUltimaInteracaoEm(), c.getCriadoEm(), c.getAtualizadoEm(),
                c.getContatos().stream().map(ContatoDtos::response).toList(),
                c.getInteracoes().stream().map(InteracaoDtos::response).toList(),
                c.getHistorico().stream()
                        .map(h -> new HistoricoResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(), h.getData()))
                        .toList());
    }
}
