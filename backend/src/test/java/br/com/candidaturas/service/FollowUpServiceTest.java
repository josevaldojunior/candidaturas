package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.domain.TipoInteracao;
import br.com.candidaturas.dto.FollowUpDtos.FollowUpItem;
import br.com.candidaturas.dto.FollowUpDtos.FollowUpsResponse;
import br.com.candidaturas.repository.InteracaoRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 24);

    @Mock
    private InteracaoRepository repository;

    @Test
    void separaVencidosDosProximosSeteDias() {
        Candidatura c = CandidaturaServiceTest.candidatura(1L, Status.ENTREVISTA_TECNICA);
        Interacao vencido = followUp(c, 10L, HOJE.minusDays(3));
        Interacao hoje = followUp(c, 11L, HOJE);
        Interacao emSete = followUp(c, 12L, HOJE.plusDays(7));
        when(repository.followUpsPendentesAte(HOJE.plusDays(7))).thenReturn(List.of(vencido, hoje, emSete));

        FollowUpsResponse r = new FollowUpService(repository, RelogioFixo.clock()).pendentes();

        verify(repository).followUpsPendentesAte(LocalDate.of(2026, 10, 31));
        assertThat(r.vencidos()).extracting(FollowUpItem::interacaoId, FollowUpItem::diasParaVencer)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(10L, -3L));
        assertThat(r.proximos()).extracting(FollowUpItem::interacaoId).containsExactly(11L, 12L);
        assertThat(r.proximos().get(1).diasParaVencer()).isEqualTo(7);
        assertThat(r.vencidos().get(0).empresa()).isEqualTo("ACME");
        assertThat(r.vencidos().get(0).status()).isEqualTo(Status.ENTREVISTA_TECNICA);
    }

    private static Interacao followUp(Candidatura c, Long id, LocalDate data) {
        Interacao i = new Interacao(LocalDateTime.of(2026, 10, 1, 9, 0), TipoInteracao.EMAIL, "Cobrar retorno");
        i.setId(id);
        i.setDataFollowUp(data);
        c.adicionarInteracao(i);
        return i;
    }
}
