package br.com.candidaturas.dto;

import br.com.candidaturas.domain.Status;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardResponse(
            long total,
            long ativas,
            long comResposta,
            double taxaResposta,
            List<EtapaFunil> funil,
            List<Contagem> porStatus,
            List<Contagem> porPlataforma,
            List<ContagemSemana> porSemana) {
    }

    /** {@code quantidade} = candidaturas que chegaram pelo menos a esta etapa; conversão é relativa à etapa anterior. */
    public record EtapaFunil(Status etapa, long quantidade, double conversaoDaAnterior, double percentualDoTotal) {
    }

    public record Contagem(String chave, long quantidade) {
    }

    public record ContagemSemana(LocalDate inicioSemana, long quantidade) {
    }
}
