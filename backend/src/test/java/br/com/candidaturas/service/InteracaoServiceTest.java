package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.domain.TipoInteracao;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoRequest;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoResponse;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.InteracaoRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteracaoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 24, 10, 0);

    @Mock
    private InteracaoRepository repository;
    @Mock
    private CandidaturaRepository candidaturaRepository;

    private InteracaoService service;

    @BeforeEach
    void setUp() {
        service = new InteracaoService(repository, candidaturaRepository, RelogioFixo.clock());
    }

    @Test
    void adicionarAtualizaUltimaInteracao() {
        Candidatura c = CandidaturaServiceTest.candidatura(1L, Status.CANDIDATADO);
        when(candidaturaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteracaoResponse r = service.adicionar(1L, new InteracaoRequest(null, TipoInteracao.EMAIL, " Enviei e-mail ",
                "Aguardar", LocalDate.of(2026, 10, 30), null));

        assertThat(r.data()).isEqualTo(AGORA);
        assertThat(r.descricao()).isEqualTo("Enviei e-mail");
        assertThat(r.followUpConcluido()).isFalse();
        assertThat(c.getUltimaInteracaoEm()).isEqualTo(AGORA);
        assertThat(c.getInteracoes()).hasSize(1);
    }

    @Test
    void excluirRecalculaUltimaInteracao() {
        Candidatura c = CandidaturaServiceTest.candidatura(1L, Status.CANDIDATADO);
        Interacao antiga = new Interacao(AGORA.minusDays(5), TipoInteracao.EMAIL, "antiga");
        Interacao recente = new Interacao(AGORA.minusDays(1), TipoInteracao.LIGACAO, "recente");
        c.adicionarInteracao(antiga);
        c.adicionarInteracao(recente);
        when(repository.findById(2L)).thenReturn(Optional.of(recente));

        service.excluir(2L);

        assertThat(c.getInteracoes()).containsExactly(antiga);
        assertThat(c.getUltimaInteracaoEm()).isEqualTo(AGORA.minusDays(5));
    }

    @Test
    void concluirFollowUp() {
        Interacao i = new Interacao(AGORA, TipoInteracao.EMAIL, "x");
        i.setDataFollowUp(LocalDate.of(2026, 10, 20));
        when(repository.findById(3L)).thenReturn(Optional.of(i));

        assertThat(service.concluirFollowUp(3L, true).followUpConcluido()).isTrue();
        assertThat(i.isFollowUpConcluido()).isTrue();
    }
}
