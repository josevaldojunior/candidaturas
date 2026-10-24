package br.com.candidaturas.dto;

import br.com.candidaturas.domain.Status;
import br.com.candidaturas.domain.TipoInteracao;

import java.time.LocalDate;
import java.util.List;

public final class FollowUpDtos {

    private FollowUpDtos() {
    }

    /** Follow-up pendente com os dados mínimos da candidatura para exibir na lista. */
    public record FollowUpItem(Long interacaoId, Long candidaturaId, String empresa, String cargo, Status status,
                               TipoInteracao tipo, String descricao, String proximoPasso, LocalDate dataFollowUp,
                               long diasParaVencer) {
    }

    public record FollowUpsResponse(List<FollowUpItem> vencidos, List<FollowUpItem> proximos) {
    }
}
