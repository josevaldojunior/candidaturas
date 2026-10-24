package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.DashboardDtos.Contagem;
import br.com.candidaturas.dto.DashboardDtos.ContagemSemana;
import br.com.candidaturas.dto.DashboardDtos.DashboardResponse;
import br.com.candidaturas.dto.DashboardDtos.EtapaFunil;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.HistoricoStatusRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CandidaturaRepository candidaturaRepository;
    @Mock
    private HistoricoStatusRepository historicoRepository;

    private DashboardService service;
    private final List<Candidatura> candidaturas = new ArrayList<>();
    private final List<HistoricoStatus> historico = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new DashboardService(candidaturaRepository, historicoRepository, RelogioFixo.clock());
        // hoje = sábado 24/10/2026; semana atual começa na segunda 19/10
        nova(1L, Status.INTERESSE, Plataforma.LINKEDIN, null);
        nova(2L, Status.CANDIDATADO, Plataforma.LINKEDIN, "2026-10-20", Status.CANDIDATADO);
        nova(3L, Status.ENTREVISTA_TECNICA, Plataforma.GUPY, "2026-10-01",
                Status.CANDIDATADO, Status.TRIAGEM_RH, Status.TESTE_TECNICO, Status.ENTREVISTA_TECNICA);
        nova(4L, Status.RECUSADA, Plataforma.LINKEDIN, "2026-10-22",
                Status.CANDIDATADO, Status.TRIAGEM_RH, Status.RECUSADA);
        nova(5L, Status.SEM_RETORNO, null, "2026-08-01", Status.CANDIDATADO, Status.SEM_RETORNO);
        nova(6L, Status.ACEITA, Plataforma.INDICACAO, "2026-10-19", Status.CANDIDATADO, Status.TRIAGEM_RH,
                Status.TESTE_TECNICO, Status.ENTREVISTA_TECNICA, Status.ENTREVISTA_FINAL, Status.PROPOSTA, Status.ACEITA);
        when(candidaturaRepository.findAll()).thenReturn(candidaturas);
        when(historicoRepository.findAll()).thenReturn(historico);
    }

    @Test
    void calculaTotaisETaxaDeResposta() {
        DashboardResponse r = service.resumo();

        assertThat(r.total()).isEqualTo(6);
        assertThat(r.ativas()).isEqualTo(2);
        // enviadas: 2,3,4,5,6 (5) — responderam: 3 (entrevista), 4 (recusada), 6 (aceita)
        assertThat(r.comResposta()).isEqualTo(3);
        assertThat(r.taxaResposta()).isEqualTo(60.0);
    }

    @Test
    void funilConsideraOHistorico() {
        List<EtapaFunil> funil = service.resumo().funil();

        assertThat(funil).extracting(EtapaFunil::etapa, EtapaFunil::quantidade, EtapaFunil::conversaoDaAnterior)
                .containsExactly(
                        tuple(Status.INTERESSE, 6L, 100.0),
                        tuple(Status.CANDIDATADO, 5L, 83.3),
                        tuple(Status.TRIAGEM_RH, 3L, 60.0),
                        tuple(Status.TESTE_TECNICO, 2L, 66.7),
                        tuple(Status.ENTREVISTA_TECNICA, 2L, 100.0),
                        tuple(Status.ENTREVISTA_FINAL, 1L, 50.0),
                        tuple(Status.PROPOSTA, 1L, 100.0),
                        tuple(Status.ACEITA, 1L, 100.0));
        assertThat(funil.get(2).percentualDoTotal()).isEqualTo(50.0);
    }

    @Test
    void agrupaPorPlataformaEPorSemana() {
        DashboardResponse r = service.resumo();

        assertThat(r.porPlataforma()).extracting(Contagem::chave, Contagem::quantidade)
                .containsExactly(tuple("LINKEDIN", 3L), tuple("GUPY", 1L), tuple("INDICACAO", 1L),
                        tuple("NAO_INFORMADA", 1L), tuple("INDEED", 0L), tuple("OUTRO", 0L));

        List<ContagemSemana> semanas = r.porSemana();
        assertThat(semanas).hasSize(12);
        assertThat(semanas.get(0).inicioSemana()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(semanas.get(11)).isEqualTo(new ContagemSemana(LocalDate.of(2026, 10, 19), 3));
        assertThat(semanas.get(8)).isEqualTo(new ContagemSemana(LocalDate.of(2026, 9, 28), 1));
        // a candidatura de 01/08 fica fora da janela e INTERESSE não conta
        assertThat(semanas.stream().mapToLong(ContagemSemana::quantidade).sum()).isEqualTo(4);
    }

    @Test
    void semCandidaturasNaoDividePorZero() {
        candidaturas.clear();
        historico.clear();

        DashboardResponse r = service.resumo();

        assertThat(r.porPlataforma()).extracting(Contagem::chave)
                .containsExactly("LINKEDIN", "GUPY", "INDEED", "INDICACAO", "OUTRO");

        assertThat(r.total()).isZero();
        assertThat(r.taxaResposta()).isZero();
        assertThat(r.funil()).allSatisfy(e -> assertThat(e.conversaoDaAnterior()).isZero());
    }

    private void nova(Long id, Status atual, Plataforma plataforma, String data, Status... passos) {
        Candidatura c = CandidaturaServiceTest.candidatura(id, atual);
        c.setPlataforma(plataforma);
        c.setDataCandidatura(data == null ? null : LocalDate.parse(data));
        Status anterior = null;
        for (Status s : passos) {
            HistoricoStatus h = new HistoricoStatus(anterior, s, LocalDateTime.of(2026, 10, 1, 9, 0));
            h.setCandidatura(c);
            historico.add(h);
            anterior = s;
        }
        candidaturas.add(c);
    }
}
