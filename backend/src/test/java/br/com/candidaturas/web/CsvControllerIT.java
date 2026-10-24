package br.com.candidaturas.web;

import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.service.CsvService;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
class CsvControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CandidaturaRepository candidaturaRepository;
    @Autowired
    private ContatoRepository contatoRepository;

    @AfterEach
    void limpar() {
        candidaturaRepository.deleteAll();
        contatoRepository.deleteAll();
    }

    @Test
    void importaExportaEReimportaSemDuplicar() throws Exception {
        String csv = String.join(",", CsvService.CABECALHO) + "\n"
                + "li-1,ACME,Dev Java,https://vagas/1,linkedin,Remoto,PJ,,,8,20/10/2026,Candidatado,Alta,v3,"
                + "Java;Spring,,Ana,ana@acme.com,,email,Enviada,,2026-10-27\n"
                + ",Beta,Backend Java,https://vagas/2,gupy,,,,,,2026-10-21,Triagem RH,,,Java,,,,,,,,\n";

        mvc.perform(multipart("/api/csv/importar").file(arquivo(csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criadas").value(2))
                .andExpect(jsonPath("$.erros").isEmpty());

        mvc.perform(get("/api/candidaturas").param("q", "acme"))
                .andExpect(jsonPath("$.content[0].origem").value("IMPORTACAO"));

        byte[] exportado = mvc.perform(get("/api/csv/exportar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("candidaturas-2026-10-24.csv")))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = new String(exportado, StandardCharsets.UTF_8);
        assertThat(texto).startsWith("﻿chave_externa,empresa,cargo");
        assertThat(texto).contains("li-1,ACME,Dev Java", "Java;Spring", "TRIAGEM_RH");

        mvc.perform(multipart("/api/csv/importar").file(arquivo(texto)))
                .andExpect(jsonPath("$.criadas").value(0))
                .andExpect(jsonPath("$.ignoradas").value(2));
        assertThat(candidaturaRepository.count()).isEqualTo(2);
    }

    @Test
    void csvInvalidoRetorna400() throws Exception {
        mvc.perform(multipart("/api/csv/importar").file(arquivo("x,y\n1,2\n")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Cabeçalho inválido")));
    }

    private static MockMultipartFile arquivo(String conteudo) {
        return new MockMultipartFile("arquivo", "candidaturas.csv", "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
