package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.IntegracaoItem;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CsvServiceTest {

    private final CsvService csv = new CsvService();

    @Test
    void leComBomStackComPontoEVirgulaEDataBrasileira() {
        String conteudo = "﻿" + String.join(",", CsvService.CABECALHO) + "\n"
                + "li-1,ACME,Dev Java,https://vagas/1,LinkedIn,Híbrido,PJ,,R$ 12k,8,24/10/2026,Triagem RH,Alta,v3,"
                + "\"Java;Spring;Angular\",\"Obs, com vírgula\",Ana,ana@acme.com,,e-mail,Respondeu,Aguardar,2026-10-30\n"
                + "\n"
                + ",Beta,Backend,,,,,,,,,,,,,,,,,,,,\n";

        List<IntegracaoItem> itens = csv.ler(new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8)));

        assertThat(itens).hasSize(2);
        IntegracaoItem a = itens.get(0);
        assertThat(a.chaveExterna()).isEqualTo("li-1");
        assertThat(a.modalidade()).isEqualTo("Híbrido");
        assertThat(a.dataCandidatura()).isEqualTo("24/10/2026");
        assertThat(a.stack()).containsExactly("Java;Spring;Angular");
        assertThat(a.observacoes()).isEqualTo("Obs, com vírgula");
        assertThat(a.contatoTelefone()).isNull();
        assertThat(a.dataFollowUp()).isEqualTo("2026-10-30");
        assertThat(itens.get(1).chaveExterna()).isNull();
        assertThat(itens.get(1).empresa()).isEqualTo("Beta");
    }

    @Test
    void aceitaSeparadorPontoEVirgulaEColunasFaltando() {
        String conteudo = "empresa;cargo;stack\nACME;Java;\"Java,Kafka\"\n";

        List<IntegracaoItem> itens = csv.ler(new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8)));

        assertThat(itens).singleElement().satisfies(i -> {
            assertThat(i.empresa()).isEqualTo("ACME");
            assertThat(i.stack()).containsExactly("Java,Kafka");
            assertThat(i.linkVaga()).isNull();
        });
    }

    @Test
    void recusaCabecalhoSemEmpresaECargo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> csv.ler(new ByteArrayInputStream("nome,link\nx,y\n".getBytes(StandardCharsets.UTF_8))))
                .withMessageContaining("Cabeçalho inválido");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> csv.ler(new ByteArrayInputStream(new byte[0])))
                .withMessageContaining("vazio");
    }

    @Test
    void exportaNoMesmoFormatoQueImporta() throws Exception {
        Candidatura c = CandidaturaServiceTest.candidatura(1L, Status.ENTREVISTA_TECNICA);
        c.setChaveExterna("li-1");
        c.setLinkVaga("https://vagas/1");
        c.setPlataforma("GUPY");
        c.setModalidade(Modalidade.REMOTO);
        c.setNota(9);
        c.setDataCandidatura(LocalDate.of(2026, 10, 1));
        c.setObservacoes("linha 1\nlinha 2, com vírgula");
        c.getStack().addAll(List.of("Spring", "Java"));
        Contato ana = new Contato();
        ana.setId(3L);
        ana.setNome("Ana");
        ana.setEmail("ana@acme.com");
        c.getContatos().add(ana);
        Interacao email = new Interacao(LocalDateTime.of(2026, 10, 2, 9, 0), TipoInteracao.EMAIL, "Enviei");
        email.setDataFollowUp(LocalDate.of(2026, 10, 9));
        c.adicionarInteracao(email);
        c.adicionarInteracao(new Interacao(LocalDateTime.of(2026, 10, 5, 9, 0), TipoInteracao.MUDANCA_STATUS, "x"));

        StringWriter saida = new StringWriter();
        csv.escrever(saida, List.of(c));

        String texto = saida.toString();
        assertThat(texto).startsWith("﻿" + String.join(",", CsvService.CABECALHO));
        List<IntegracaoItem> lidos = csv.ler(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
        assertThat(lidos).singleElement().satisfies(i -> {
            assertThat(i.chaveExterna()).isEqualTo("li-1");
            assertThat(i.status()).isEqualTo("ENTREVISTA_TECNICA");
            assertThat(i.dataCandidatura()).isEqualTo("2026-10-01");
            assertThat(i.stack()).containsExactly("Java;Spring");
            assertThat(i.observacoes()).isEqualTo("linha 1\nlinha 2, com vírgula");
            assertThat(i.contatoEmail()).isEqualTo("ana@acme.com");
            assertThat(i.tipoInteracao()).isEqualTo("EMAIL");
            assertThat(i.dataFollowUp()).isEqualTo("2026-10-09");
        });
    }
}
