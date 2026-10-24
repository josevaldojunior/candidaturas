package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Configuracao;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.domain.TipoInteracao;
import br.com.candidaturas.dto.SemRetornoDtos.ResultadoSemRetorno;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ConfiguracaoRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemRetornoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 24, 10, 0);

    @Mock
    private CandidaturaRepository candidaturaRepository;
    @Mock
    private ConfiguracaoRepository configuracaoRepository;
    @Mock
    private ContatoRepository contatoRepository;

    private SemRetornoService service;

    private Candidatura paradaHa24Dias;
    private Candidatura comInteracaoRecente;
    private Candidatura semInteracaoComDataAntiga;
    private Candidatura recemCriada;

    @BeforeEach
    void setUp() {
        var candidaturaService = new CandidaturaService(candidaturaRepository, contatoRepository, RelogioFixo.clock());
        service = new SemRetornoService(candidaturaRepository, configuracaoRepository, candidaturaService,
                RelogioFixo.clock());

        paradaHa24Dias = CandidaturaServiceTest.candidatura(1L, Status.CANDIDATADO);
        paradaHa24Dias.setUltimaInteracaoEm(LocalDateTime.of(2026, 9, 30, 9, 0));

        comInteracaoRecente = CandidaturaServiceTest.candidatura(2L, Status.TRIAGEM_RH);
        comInteracaoRecente.setUltimaInteracaoEm(LocalDateTime.of(2026, 10, 10, 9, 0));

        semInteracaoComDataAntiga = CandidaturaServiceTest.candidatura(3L, Status.ENTREVISTA_FINAL);
        semInteracaoComDataAntiga.setDataCandidatura(LocalDate.of(2026, 9, 1));

        recemCriada = CandidaturaServiceTest.candidatura(4L, Status.CANDIDATADO);
        recemCriada.setCriadoEm(LocalDateTime.of(2026, 10, 20, 8, 0));

        lenient().when(candidaturaRepository.findByStatusIn(Status.AGUARDANDO_RETORNO))
                .thenReturn(List.of(paradaHa24Dias, comInteracaoRecente, semInteracaoComDataAntiga, recemCriada));
    }

    @Test
    void marcaSoAsParadasHaMaisDeXDias() {
        configurarDias("21");

        ResultadoSemRetorno r = service.executar();

        assertThat(r.dias()).isEqualTo(21);
        assertThat(r.ids()).containsExactly(1L, 3L);
        assertThat(r.executadoEm()).isEqualTo(AGORA);
        assertThat(paradaHa24Dias.getStatus()).isEqualTo(Status.SEM_RETORNO);
        assertThat(semInteracaoComDataAntiga.getStatus()).isEqualTo(Status.SEM_RETORNO);
        assertThat(comInteracaoRecente.getStatus()).isEqualTo(Status.TRIAGEM_RH);
        assertThat(recemCriada.getStatus()).isEqualTo(Status.CANDIDATADO);
        verify(candidaturaRepository, times(2)).save(any());
    }

    @Test
    void gravaHistoricoEInteracaoAoMarcar() {
        configurarDias("21");

        service.executar();

        assertThat(paradaHa24Dias.getHistorico()).singleElement().satisfies(h -> {
            assertThat(h.getStatusAnterior()).isEqualTo(Status.CANDIDATADO);
            assertThat(h.getStatusNovo()).isEqualTo(Status.SEM_RETORNO);
        });
        assertThat(paradaHa24Dias.getInteracoes()).singleElement().satisfies(i -> {
            assertThat(i.getTipo()).isEqualTo(TipoInteracao.MUDANCA_STATUS);
            assertThat(i.getDescricao()).contains("21 dias");
        });
        assertThat(paradaHa24Dias.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void respeitaODiaConfigurado() {
        configurarDias("10");

        assertThat(service.executar().ids()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void usaPadraoQuandoNaoHaConfiguracaoValida() {
        when(configuracaoRepository.findById(SemRetornoService.CHAVE_DIAS)).thenReturn(Optional.empty());
        assertThat(service.dias()).isEqualTo(21);

        configurarDias("abc");
        assertThat(service.dias()).isEqualTo(21);
    }

    @Test
    void salvaDiasValidando() {
        when(configuracaoRepository.findById(SemRetornoService.CHAVE_DIAS)).thenReturn(Optional.empty());

        assertThat(service.salvarDias(30)).isEqualTo(30);
        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        verify(configuracaoRepository).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEqualTo("30");

        assertThatThrownBy(() -> service.salvarDias(0)).isInstanceOf(RegraNegocioException.class);
    }

    private void configurarDias(String valor) {
        when(configuracaoRepository.findById(SemRetornoService.CHAVE_DIAS))
                .thenReturn(Optional.of(new Configuracao(SemRetornoService.CHAVE_DIAS, valor)));
    }
}
