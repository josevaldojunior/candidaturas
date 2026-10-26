package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.IntegracaoDtos.ExisteResponse;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoItem;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.support.RelogioFixo;
import br.com.candidaturas.support.UsuarioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntegracaoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 24, 10, 0);

    @Mock
    private CandidaturaRepository repository;
    @Mock
    private ContatoRepository contatoRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private IntegracaoService service;

    @BeforeEach
    void setUp() {
        var candidaturaService = new CandidaturaService(repository, contatoRepository, new UsuarioFixo(), RelogioFixo.clock());
        service = new IntegracaoService(repository, contatoRepository, candidaturaService, transactionManager,
                new UsuarioFixo(), RelogioFixo.clock());
        when(repository.findByUsuarioIdAndChaveExterna(any(), any())).thenReturn(Optional.empty());
        when(repository.findFirstByUsuarioIdAndLinkVagaOrderByIdAsc(any(), any())).thenReturn(Optional.empty());
        when(repository.findFirstByUsuarioIdAndEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contatoRepository.findFirstByUsuarioIdAndEmailIgnoreCaseOrderByIdAsc(any(), any())).thenReturn(Optional.empty());
        when(contatoRepository.findByUsuarioIdAndTelefoneIsNotNull(any())).thenReturn(List.of());
        when(contatoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criaComOrigemAgenteNormalizandoEnums() {
        ResultadoImportacao r = service.processar(List.of(item("vaga-1", "https://vagas/1", "Triagem RH")
                .comModalidade("Híbrido").comPlataforma("linkedin").comInteracao("e-mail", "Recrutadora respondeu", "2026-10-30")
                .build()), Origem.AGENTE);

        assertThat(r).isEqualTo(new ResultadoImportacao(1, 0, 0, List.of()));
        Candidatura c = salva();
        assertThat(c.getOrigem()).isEqualTo(Origem.AGENTE);
        assertThat(c.getUsuarioId()).isEqualTo(UsuarioFixo.ID);
        assertThat(c.getChaveExterna()).isEqualTo("vaga-1");
        assertThat(c.getStatus()).isEqualTo(Status.TRIAGEM_RH);
        assertThat(c.getModalidade()).isEqualTo(Modalidade.HIBRIDO);
        assertThat(c.getPlataforma()).isEqualTo(Plataforma.LINKEDIN_VAGAS);
        assertThat(c.getTipoContrato()).isEqualTo(TipoContrato.PJ);
        assertThat(c.getPrioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(c.getStack()).containsExactly("Java", "Spring");
        assertThat(c.getDataCandidatura()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(c.getHistorico()).hasSize(1);
        assertThat(c.getInteracoes()).singleElement().satisfies(i -> {
            assertThat(i.getTipo()).isEqualTo(TipoInteracao.EMAIL);
            assertThat(i.getDataFollowUp()).isEqualTo(LocalDate.of(2026, 10, 30));
        });
        assertThat(c.getContatos()).singleElement().satisfies(ct -> {
            assertThat(ct.getNome()).isEqualTo("Ana");
            assertThat(ct.getCargo()).isEqualTo(CargoContato.RECRUTADOR);
            assertThat(ct.getEmpresa()).isEqualTo("ACME");
            assertThat(ct.getUsuarioId()).isEqualTo(UsuarioFixo.ID);
        });
    }

    @Test
    void statusPadraoEhCandidatado() {
        service.processar(List.of(item(null, "https://vagas/2", null).build()), Origem.AGENTE);

        assertThat(salva().getStatus()).isEqualTo(Status.CANDIDATADO);
    }

    @Test
    void atualizaSoOsCamposEnviadosEGravaHistorico() {
        Candidatura existente = existente();
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));
        IntegracaoItem soStatus = new Builder().chave("vaga-1").status("Entrevista técnica").build();

        ResultadoImportacao r = service.processar(List.of(soStatus), Origem.AGENTE);

        assertThat(r.atualizadas()).isEqualTo(1);
        assertThat(existente.getEmpresa()).isEqualTo("ACME");
        assertThat(existente.getLinkVaga()).isEqualTo("https://vagas/1");
        assertThat(existente.getStatus()).isEqualTo(Status.ENTREVISTA_TECNICA);
        assertThat(existente.getHistorico()).singleElement()
                .satisfies(h -> assertThat(h.getStatusAnterior()).isEqualTo(Status.CANDIDATADO));
        assertThat(existente.getInteracoes()).singleElement()
                .satisfies(i -> assertThat(i.getTipo()).isEqualTo(TipoInteracao.MUDANCA_STATUS));
        assertThat(existente.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(existente.getOrigem()).isEqualTo(Origem.MANUAL);
    }

    @Test
    void localizaPeloLinkQuandoNaoHaChaveEGuardaAChaveNova() {
        Candidatura existente = existente();
        existente.setChaveExterna(null);
        when(repository.findFirstByUsuarioIdAndLinkVagaOrderByIdAsc(UsuarioFixo.ID, "https://vagas/1")).thenReturn(Optional.of(existente));

        ResultadoImportacao r = service.processar(List.of(new Builder().chave("li-99").link("https://vagas/1").build()),
                Origem.AGENTE);

        assertThat(r.atualizadas()).isEqualTo(1);
        assertThat(existente.getChaveExterna()).isEqualTo("li-99");
        verify(repository, never()).save(argThat(c -> c != existente));
    }

    @Test
    void itemIgualAoGravadoEhIgnorado() {
        Candidatura existente = existente();
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));

        ResultadoImportacao r = service.processar(List.of(new Builder().chave("vaga-1").empresa("ACME")
                .status("candidatado").stack("java;spring").build()), Origem.AGENTE);

        assertThat(r).isEqualTo(new ResultadoImportacao(0, 0, 1, List.of()));
        verify(repository, never()).save(any());
    }

    @Test
    void naoDuplicaInteracaoNemContatoAoRepetir() {
        Candidatura existente = existente();
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));
        IntegracaoItem comInteracao = item("vaga-1", "https://vagas/1", null)
                .comInteracao("ligação", "Conversa com RH", null).build();

        assertThat(service.processar(List.of(comInteracao), Origem.AGENTE).atualizadas()).isEqualTo(1);
        assertThat(service.processar(List.of(comInteracao), Origem.AGENTE).ignoradas()).isEqualTo(1);

        assertThat(existente.getInteracoes()).hasSize(1);
        assertThat(existente.getContatos()).hasSize(1);
    }

    @Test
    void reaproveitaContatoPorEmailOuTelefone() {
        Contato ana = new Contato();
        ana.setId(7L);
        ana.setNome("Ana Souza");
        ana.setEmail("ANA@acme.com");
        ana.setCargo(CargoContato.RECRUTADOR);
        when(contatoRepository.findFirstByUsuarioIdAndEmailIgnoreCaseOrderByIdAsc(UsuarioFixo.ID, "ana@acme.com")).thenReturn(Optional.of(ana));

        service.processar(List.of(item("vaga-1", null, null).build()), Origem.AGENTE);

        assertThat(salva().getContatos()).containsExactly(ana);
        assertThat(ana.getTelefone()).isEqualTo("(11) 99999-0000");

        Contato bruno = new Contato();
        bruno.setId(8L);
        bruno.setNome("Bruno");
        bruno.setTelefone("+55 11 98888-7777");
        bruno.setCargo(CargoContato.TECH_LEAD);
        when(contatoRepository.findByUsuarioIdAndTelefoneIsNotNull(any())).thenReturn(List.of(bruno));

        service.processar(List.of(new Builder().chave("vaga-2").empresa("Beta").cargo("Java")
                .contato("Bruno", null, "11988887777").build()), Origem.AGENTE);

        verify(contatoRepository, never()).save(any());
    }

    @Test
    void erroEmUmItemNaoDerrubaOsOutros() {
        List<IntegracaoItem> itens = Arrays.asList(
                item("ok-1", null, null).build(),
                new Builder().chave("sem-empresa").cargo("Java").build(),
                item("status-ruim", null, "Contratado amanhã").build(),
                null,
                item("nota-ruim", null, null).nota("15").build(),
                item("data-ruim", null, null).data("31/31/2026").build());

        ResultadoImportacao r = service.processar(itens, Origem.AGENTE);

        assertThat(r.criadas()).isEqualTo(1);
        assertThat(r.erros()).containsExactly(
                "Item 2: empresa é obrigatória",
                "Item 3: valor inválido para status: 'Contratado amanhã'",
                "Item 4: item vazio",
                "Item 5: nota deve estar entre 1 e 10: '15'",
                "Item 6: dataCandidatura inválida: '31/31/2026' (use yyyy-MM-dd ou dd/MM/yyyy)");
    }

    @Test
    void observacaoComPrefixoPendentePreenchePendencia() {
        service.processar(List.of(item("p-1", null, null).observacoes("PENDENTE:  enviar teste até sexta").build()),
                Origem.AGENTE);

        Candidatura c = salva();
        assertThat(c.getPendencia()).isEqualTo("enviar teste até sexta");
        assertThat(c.getObservacoes()).isEqualTo("PENDENTE:  enviar teste até sexta");
    }

    @Test
    void pendenciaAtualizaAoReenviarEObservacaoComumNaoMexe() {
        Candidatura existente = existente();
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));

        assertThat(service.processar(List.of(new Builder().chave("vaga-1").observacoes("pendente: mandar portfólio").build()),
                Origem.AGENTE).atualizadas()).isEqualTo(1);
        assertThat(existente.getPendencia()).isEqualTo("mandar portfólio");

        service.processar(List.of(new Builder().chave("vaga-1").observacoes("Recrutadora simpática").build()), Origem.AGENTE);
        assertThat(existente.getPendencia()).isEqualTo("mandar portfólio");
    }

    @Test
    void reenviarMesmaObservacaoNaoRessuscitaPendenciaLimpa() {
        Candidatura existente = existente();
        existente.setObservacoes("PENDENTE: mandar portfólio");
        existente.setPendencia(null); // limpa na tela
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));

        ResultadoImportacao r = service.processar(
                List.of(new Builder().chave("vaga-1").observacoes("PENDENTE: mandar portfólio").build()), Origem.AGENTE);

        assertThat(r.ignoradas()).isEqualTo(1);
        assertThat(existente.getPendencia()).isNull();
    }

    @Test
    void agenteMarcandoCandidatadoConcluiAPendencia() {
        Candidatura existente = existente();
        existente.setStatus(Status.INTERESSE);
        existente.setPendencia("Etapa de perguntas não carrega");
        when(repository.findByUsuarioIdAndChaveExterna(UsuarioFixo.ID, "vaga-1")).thenReturn(Optional.of(existente));

        service.processar(List.of(new Builder().chave("vaga-1").status("Candidatado").build()), Origem.AGENTE);

        assertThat(existente.getStatus()).isEqualTo(Status.CANDIDATADO);
        assertThat(existente.getPendencia()).isNull();
    }

    @Test
    void prefixoSemTextoOuNoMeioNaoGeraPendencia() {
        assertThat(IntegracaoService.Dados.pendenciaDe("PENDENTE:   ")).isNull();
        assertThat(IntegracaoService.Dados.pendenciaDe("Status PENDENTE: x")).isNull();
        assertThat(IntegracaoService.Dados.pendenciaDe(null)).isNull();
        assertThat(IntegracaoService.Dados.pendenciaDe("  PENDENTE:ligar")).isEqualTo("ligar");
    }

    @Test
    void existeProcuraPorLinkOuEmpresaECargo() {
        Candidatura c = existente();
        when(repository.findFirstByUsuarioIdAndLinkVagaOrderByIdAsc(UsuarioFixo.ID, "https://vagas/1")).thenReturn(Optional.of(c));
        when(repository.findFirstByUsuarioIdAndEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(UsuarioFixo.ID, "acme", "dev java")).thenReturn(Optional.of(c));

        ExisteResponse porLink = service.existe("https://vagas/1", null, null);
        assertThat(porLink.existe()).isTrue();
        assertThat(porLink.candidatura().id()).isEqualTo(10L);

        assertThat(service.existe(null, "acme", "dev java").existe()).isTrue();
        assertThat(service.existe("https://outra", "acme", "dev java").existe()).isFalse();
        assertThat(service.existe(null, null, null)).isEqualTo(new ExisteResponse(false, null));
    }

    private Candidatura salva() {
        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(0);
    }

    private static Candidatura existente() {
        Candidatura c = CandidaturaServiceTest.candidatura(10L, Status.CANDIDATADO);
        c.setChaveExterna("vaga-1");
        c.setLinkVaga("https://vagas/1");
        c.getStack().addAll(List.of("Java", "Spring"));
        return c;
    }

    private static Builder item(String chave, String link, String status) {
        return new Builder().chave(chave).link(link).status(status).empresa("ACME").cargo("Dev Java")
                .contrato("pj").stack("Java; Spring").contato("Ana", "ana@acme.com", "(11) 99999-0000");
    }

    /** Monta itens de teste sem repetir os 23 campos do contrato. */
    static final class Builder {
        private String chave, empresa, cargo, link, plataforma, modalidade, contrato, nota, data, status;
        private String stack, contatoNome, contatoEmail, contatoTelefone, tipoInteracao, descricao, followUp;
        private String observacoes;

        Builder chave(String v) { chave = v; return this; }
        Builder empresa(String v) { empresa = v; return this; }
        Builder cargo(String v) { cargo = v; return this; }
        Builder link(String v) { link = v; return this; }
        Builder comPlataforma(String v) { plataforma = v; return this; }
        Builder comModalidade(String v) { modalidade = v; return this; }
        Builder contrato(String v) { contrato = v; return this; }
        Builder nota(String v) { nota = v; return this; }
        Builder data(String v) { data = v; return this; }
        Builder status(String v) { status = v; return this; }
        Builder stack(String v) { stack = v; return this; }
        Builder observacoes(String v) { observacoes = v; return this; }

        Builder contato(String nome, String email, String telefone) {
            contatoNome = nome;
            contatoEmail = email;
            contatoTelefone = telefone;
            return this;
        }

        Builder comInteracao(String tipo, String desc, String fu) {
            tipoInteracao = tipo;
            descricao = desc;
            followUp = fu;
            return this;
        }

        IntegracaoItem build() {
            return new IntegracaoItem(chave, empresa, cargo, link, plataforma, modalidade, contrato, null, null, nota,
                    data, status, null, null, observacoes, stack == null ? null : List.of(stack), contatoNome, contatoEmail,
                    contatoTelefone, tipoInteracao, descricao, null, followUp);
        }
    }
}
