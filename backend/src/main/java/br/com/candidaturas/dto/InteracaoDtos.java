package br.com.candidaturas.dto;

import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.domain.TipoInteracao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class InteracaoDtos {

    private InteracaoDtos() {
    }

    public record InteracaoRequest(
            LocalDateTime data,
            @NotNull TipoInteracao tipo,
            @Size(max = 4000) String descricao,
            @Size(max = 1000) String proximoPasso,
            LocalDate dataFollowUp,
            Boolean followUpConcluido) {
    }

    public record InteracaoResponse(Long id, LocalDateTime data, TipoInteracao tipo, String descricao,
                                    String proximoPasso, LocalDate dataFollowUp, boolean followUpConcluido) {
    }

    public static InteracaoResponse response(Interacao i) {
        return new InteracaoResponse(i.getId(), i.getData(), i.getTipo(), i.getDescricao(), i.getProximoPasso(),
                i.getDataFollowUp(), i.isFollowUpConcluido());
    }
}
