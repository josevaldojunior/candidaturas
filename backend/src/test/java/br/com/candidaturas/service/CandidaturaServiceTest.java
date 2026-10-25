package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.CandidaturaDtos.CandidaturaDetalhe;
import br.com.candidaturas.dto.CandidaturaDtos.CandidaturaRequest;
import br.com.candidaturas.dto.CandidaturaDtos.MudancaStatusRequest;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidaturaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 24, 10, 0);

    @Mock
    private CandidaturaRepository repository;
    @Mock
    private ContatoRepository contatoRepository;

    private CandidaturaService service;

    @BeforeEach
    void setUp() {
        service = new CandidaturaService(repository, contatoRepository, RelogioFixo.clock());
    }

    @Test
    void criaComPadroesEHistoricoInicial() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        CandidaturaDetalhe d = service.criar(request("ACME", null, null));

        assertThat(d.status()).isEqualTo(Status.CANDIDATADO);
        assertThat(d.prioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(d.origem()).isEqualTo(Origem.MANUAL);
        assertThat(d.dataCandidatura()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(d.criadoEm()).isEqualTo(AGORA);
        assertThat(d.stack()).containsExactly("Java", "Spring");
        assertThat(d.historico()).singleElement()
                .satisfies(h -> {
                    assertThat(h.statusAnterior()).isNull();
                    assertThat(h.statusNovo()).isEqualTo(Status.CANDIDATADO);
                });
    }

    @Test
    void interesseNaoGanhaDataDeCandidatura() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        CandidaturaDetalhe d = service.criar(request("ACME", Status.INTERESSE, null));

        assertThat(d.dataCandidatura()).isNull();
    }

    @Test
    void recusaChaveExternaDuplicada() {
        Candidatura outra = candidatura(99L, Status.CANDIDATADO);
        when(repository.findByChaveExterna("vaga-1")).thenReturn(Optional.of(outra));

        assertThatThrownBy(() -> service.criar(request("ACME", null, "vaga-1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("99");
    }

    @Test
    void mudarStatusGravaHistoricoEInteracao() {
        Candidatura c = candidatura(1L, Status.CANDIDATADO);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        CandidaturaDetalhe d = service.mudarStatus(1L, new MudancaStatusRequest(Status.TRIAGEM_RH, "RH ligou"));

        assertThat(d.status()).isEqualTo(Status.TRIAGEM_RH);
        assertThat(c.getHistorico()).singleElement().satisfies(h -> {
            assertThat(h.getStatusAnterior()).isEqualTo(Status.CANDIDATADO);
            assertThat(h.getStatusNovo()).isEqualTo(Status.TRIAGEM_RH);
            assertThat(h.getData()).isEqualTo(AGORA);
        });
        assertThat(c.getInteracoes()).singleElement().satisfies(i -> {
            assertThat(i.getTipo()).isEqualTo(TipoInteracao.MUDANCA_STATUS);
            assertThat(i.getDescricao()).contains("CANDIDATADO", "TRIAGEM_RH", "RH ligou");
        });
        assertThat(c.getUltimaInteracaoEm()).isEqualTo(AGORA);
        assertThat(c.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void mesmoStatusNaoGeraHistorico() {
        Candidatura c = candidatura(1L, Status.CANDIDATADO);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.mudarStatus(1L, new MudancaStatusRequest(Status.CANDIDATADO, null));

        assertThat(c.getHistorico()).isEmpty();
        assertThat(c.getInteracoes()).isEmpty();
    }

    @Test
    void sairDeInteressePreencheDataDeCandidatura() {
        Candidatura c = candidatura(1L, Status.INTERESSE);

        service.aplicarStatus(c, Status.CANDIDATADO, null);

        assertThat(c.getDataCandidatura()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void atualizarComNovoStatusGravaHistorico() {
        Candidatura c = candidatura(1L, Status.CANDIDATADO);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.atualizar(1L, request("ACME Atualizada", Status.TESTE_TECNICO, null));

        assertThat(c.getEmpresa()).isEqualTo("ACME Atualizada");
        assertThat(c.getStatus()).isEqualTo(Status.TESTE_TECNICO);
        assertThat(c.getHistorico()).hasSize(1);
    }

    @Test
    void limparOCampoAoEditarRemoveAPendencia() {
        Candidatura c = candidatura(1L, Status.CANDIDATADO);
        c.setPendencia("Enviar portfólio");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        CandidaturaDetalhe d = service.atualizar(1L, request("ACME", Status.CANDIDATADO, null));

        assertThat(d.pendencia()).isNull();
        assertThat(c.getPendencia()).isNull();
    }

    @Test
    void sairDeInteresseConcluiAPendenciaERegistraNoHistorico() {
        Candidatura c = candidatura(1L, Status.INTERESSE);
        c.setPendencia("Pede RG e nome dos pais");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        CandidaturaDetalhe d = service.mudarStatus(1L, new MudancaStatusRequest(Status.CANDIDATADO, null));

        assertThat(d.pendencia()).isNull();
        assertThat(c.getDataCandidatura()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(c.getInteracoes()).singleElement()
                .satisfies(i -> assertThat(i.getDescricao()).contains("Pendência concluída: Pede RG e nome dos pais"));
    }

    @Test
    void mudarEntreOutrasEtapasMantemAPendencia() {
        Candidatura c = candidatura(1L, Status.TRIAGEM_RH);
        c.setPendencia("Enviar teste técnico");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.mudarStatus(1L, new MudancaStatusRequest(Status.TESTE_TECNICO, null));

        assertThat(c.getPendencia()).isEqualTo("Enviar teste técnico");
    }

    @Test
    void editarParaCandidatadoLimpaAPendenciaMesmoComOTextoAntigoNoFormulario() {
        Candidatura c = candidatura(1L, Status.INTERESSE);
        c.setPendencia("Pede CPF");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.atualizar(1L, comPendencia(request("ACME", Status.CANDIDATADO, null), "Pede CPF"));

        assertThat(c.getStatus()).isEqualTo(Status.CANDIDATADO);
        assertThat(c.getPendencia()).isNull();
    }

    @Test
    void formularioQueJaApagouOCampoAindaRegistraAPendenciaConcluida() {
        Candidatura c = candidatura(1L, Status.INTERESSE);
        c.setPendencia("Pede CPF");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.atualizar(1L, comPendencia(request("ACME", Status.CANDIDATADO, null), ""));

        assertThat(c.getPendencia()).isNull();
        assertThat(c.getInteracoes()).singleElement()
                .satisfies(i -> assertThat(i.getDescricao()).contains("Pendência concluída: Pede CPF"));
    }

    @Test
    void editarSemMudarStatusAindaPermiteTrocarAPendencia() {
        Candidatura c = candidatura(1L, Status.TRIAGEM_RH);
        c.setPendencia("Antiga");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.atualizar(1L, comPendencia(request("ACME", Status.TESTE_TECNICO, null), "Nova"));

        assertThat(c.getPendencia()).isEqualTo("Nova");
    }

    @Test
    void pendenciaNovaEscritaNaMesmaEdicaoEhMantida() {
        Candidatura c = candidatura(1L, Status.INTERESSE);
        c.setPendencia("Pede CPF");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.saveAndFlush(c)).thenReturn(c);

        service.atualizar(1L, comPendencia(request("ACME", Status.CANDIDATADO, null), "Aguardar retorno do RH"));

        assertThat(c.getPendencia()).isEqualTo("Aguardar retorno do RH");
    }

    @Test
    void buscarInexistenteLancaNaoEncontrado() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(5L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private static CandidaturaRequest request(String empresa, Status status, String chave) {
        return new CandidaturaRequest(empresa, "Dev Java", "https://vagas/1", Plataforma.LINKEDIN_VAGAS, Modalidade.REMOTO,
                TipoContrato.PJ, null, null, 8, Set.of("Java", "Spring"), null, status, null, "v3", null, chave, null, null);
    }

    private static CandidaturaRequest comPendencia(CandidaturaRequest r, String pendencia) {
        return new CandidaturaRequest(r.empresa(), r.cargo(), r.linkVaga(), r.plataforma(), r.modalidade(),
                r.tipoContrato(), r.faixaSalarial(), r.pretensaoSalarial(), r.nota(), r.stack(), r.dataCandidatura(),
                r.status(), r.prioridade(), r.curriculoVersao(), r.observacoes(), r.chaveExterna(), r.contatoIds(),
                pendencia);
    }

    static Candidatura candidatura(Long id, Status status) {
        Candidatura c = new Candidatura();
        c.setId(id);
        c.setEmpresa("ACME");
        c.setCargo("Dev Java");
        c.setStatus(status);
        c.setPrioridade(Prioridade.MEDIA);
        c.setOrigem(Origem.MANUAL);
        c.setCriadoEm(AGORA.minusDays(30));
        c.setAtualizadoEm(AGORA.minusDays(30));
        return c;
    }
}
