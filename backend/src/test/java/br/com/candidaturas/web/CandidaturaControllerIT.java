package br.com.candidaturas.web;

import br.com.candidaturas.support.RelogioFixo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
@Transactional
class CandidaturaControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void criaBuscaEAtualiza() throws Exception {
        long id = criar("""
                {"empresa":"ACME","cargo":"Dev Java Pleno","linkVaga":"https://vagas/acme/1","plataforma":"LINKEDIN_VAGAS",
                 "modalidade":"REMOTO","tipoContrato":"PJ","nota":8,"stack":["Java","Spring"],"prioridade":"ALTA"}
                """);

        mvc.perform(get("/api/candidaturas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa").value("ACME"))
                .andExpect(jsonPath("$.status").value("CANDIDATADO"))
                .andExpect(jsonPath("$.origem").value("MANUAL"))
                .andExpect(jsonPath("$.dataCandidatura").value("2026-10-24"))
                .andExpect(jsonPath("$.stack", contains("Java", "Spring")))
                .andExpect(jsonPath("$.historico", hasSize(1)));

        mvc.perform(put("/api/candidaturas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"empresa":"ACME","cargo":"Dev Java Sênior","status":"TRIAGEM_RH","stack":["Java"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Dev Java Sênior"))
                .andExpect(jsonPath("$.status").value("TRIAGEM_RH"))
                .andExpect(jsonPath("$.historico", hasSize(2)))
                .andExpect(jsonPath("$.interacoes[0].tipo").value("MUDANCA_STATUS"));
    }

    @Test
    void mudarStatusGravaHistoricoEInteracao() throws Exception {
        long id = criar("""
                {"empresa":"Beta","cargo":"Backend Java"}
                """);

        mvc.perform(patch("/api/candidaturas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENTREVISTA_TECNICA\",\"observacao\":\"marcada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREVISTA_TECNICA"))
                .andExpect(jsonPath("$.historico[1].statusAnterior").value("CANDIDATADO"))
                .andExpect(jsonPath("$.historico[1].statusNovo").value("ENTREVISTA_TECNICA"))
                .andExpect(jsonPath("$.interacoes[0].tipo").value("MUDANCA_STATUS"))
                .andExpect(jsonPath("$.interacoes[0].id").isNumber())
                .andExpect(jsonPath("$.ultimaInteracaoEm").value("2026-10-24T10:00:00"));
    }

    @Test
    void listaComFiltros() throws Exception {
        criar("{\"empresa\":\"ACME\",\"cargo\":\"Java Dev\",\"plataforma\":\"GUPY\",\"stack\":[\"Java\",\"Angular\"],\"prioridade\":\"ALTA\",\"dataCandidatura\":\"2026-10-01\"}");
        criar("{\"empresa\":\"Beta\",\"cargo\":\"Kotlin Dev\",\"plataforma\":\"LINKEDIN_VAGAS\",\"stack\":[\"Kotlin\"],\"dataCandidatura\":\"2026-09-01\"}");
        criar("{\"empresa\":\"Gama\",\"cargo\":\"Fullstack\",\"plataforma\":\"LINKEDIN_VAGAS\",\"status\":\"INTERESSE\",\"observacoes\":\"usa Spring Boot\"}");

        mvc.perform(get("/api/candidaturas").param("plataforma", "LINKEDIN_VAGAS"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/candidaturas").param("stack", "angular"))
                .andExpect(jsonPath("$.content[*].empresa", contains("ACME")));
        mvc.perform(get("/api/candidaturas").param("q", "spring boot"))
                .andExpect(jsonPath("$.content[*].empresa", contains("Gama")));
        mvc.perform(get("/api/candidaturas").param("status", "CANDIDATADO", "INTERESSE").param("empresa", "et"))
                .andExpect(jsonPath("$.content[*].empresa", contains("Beta")));
        mvc.perform(get("/api/candidaturas").param("de", "2026-09-15").param("ate", "2026-10-31"))
                .andExpect(jsonPath("$.content[*].empresa", contains("ACME")));
        mvc.perform(get("/api/candidaturas").param("prioridade", "ALTA"))
                .andExpect(jsonPath("$.content[*].empresa", contains("ACME")));
        mvc.perform(get("/api/candidaturas/stacks"))
                .andExpect(jsonPath("$", hasItems("Java", "Angular", "Kotlin")));
    }

    @Test
    void kanbanAgrupaPorStatusNaOrdemDoPipeline() throws Exception {
        criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\",\"status\":\"PROPOSTA\"}");

        mvc.perform(get("/api/candidaturas/kanban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERESSE"))
                .andExpect(jsonPath("$[6].status").value("PROPOSTA"))
                .andExpect(jsonPath("$[6].candidaturas[0].empresa").value("ACME"));
    }

    @Test
    void interacoesEContatos() throws Exception {
        long id = criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\"}");
        String contato = mvc.perform(post("/api/contatos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"cargo\":\"RECRUTADOR\",\"email\":\"ana@acme.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long contatoId = mapper.readTree(contato).get("id").asLong();

        mvc.perform(post("/api/candidaturas/{id}/contatos/{c}", id, contatoId))
                .andExpect(jsonPath("$.contatos[0].nome").value("Ana"));

        String interacao = mvc.perform(post("/api/candidaturas/{id}/interacoes", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMAIL\",\"descricao\":\"Respondi\",\"dataFollowUp\":\"2026-10-28\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followUpConcluido").value(false))
                .andReturn().getResponse().getContentAsString();
        long interacaoId = mapper.readTree(interacao).get("id").asLong();

        mvc.perform(patch("/api/interacoes/{id}/follow-up", interacaoId))
                .andExpect(jsonPath("$.followUpConcluido").value(true));

        mvc.perform(delete("/api/candidaturas/{id}/contatos/{c}", id, contatoId))
                .andExpect(jsonPath("$.contatos", hasSize(0)));
    }

    @Test
    void validacaoRetornaErroPadronizado() throws Exception {
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"\",\"cargo\":\"Java\",\"nota\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.caminho").value("/api/candidaturas"))
                .andExpect(jsonPath("$.campos[*].campo", containsInAnyOrder("empresa", "nota")));
    }

    @Test
    void naoEncontradoEChaveDuplicada() throws Exception {
        mvc.perform(get("/api/candidaturas/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem", containsString("999999")));

        criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\",\"chaveExterna\":\"k-1\"}");
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"Outra\",\"cargo\":\"Java\",\"chaveExterna\":\"k-1\"}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/candidaturas").param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("status")));
    }

    @Test
    void pendenciaFiltroELimpeza() throws Exception {
        long id = criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\",\"pendencia\":\"Enviar portfólio\"}");
        criar("{\"empresa\":\"Beta\",\"cargo\":\"Java\"}");

        mvc.perform(get("/api/candidaturas").param("pendentes", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].pendencia").value("Enviar portfólio"));
        mvc.perform(get("/api/dashboard")).andExpect(jsonPath("$.pendentes").value(1));

        mvc.perform(put("/api/candidaturas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"ACME\",\"cargo\":\"Java\",\"pendencia\":\"\"}"))
                .andExpect(jsonPath("$.pendencia").value(nullValue()));
        mvc.perform(get("/api/candidaturas").param("pendentes", "true"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/dashboard")).andExpect(jsonPath("$.pendentes").value(0));
    }

    @Test
    void moverDeInteresseParaCandidatadoTiraDosPendentes() throws Exception {
        long id = criar("{\"empresa\":\"PagBank\",\"cargo\":\"Java\",\"status\":\"INTERESSE\",\"pendencia\":\"Pede RG\"}");
        mvc.perform(get("/api/candidaturas").param("pendentes", "true")).andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(patch("/api/candidaturas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANDIDATADO\"}"))
                .andExpect(jsonPath("$.pendencia").value(nullValue()))
                .andExpect(jsonPath("$.interacoes[0].descricao", containsString("Pendência concluída: Pede RG")));

        mvc.perform(get("/api/candidaturas").param("pendentes", "true")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/dashboard")).andExpect(jsonPath("$.pendentes").value(0));
    }

    @Test
    void excluir() throws Exception {
        long id = criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\"}");
        mvc.perform(delete("/api/candidaturas/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/candidaturas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void actuatorESwagger() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/candidaturas']").exists());
    }

    private long criar(String json) throws Exception {
        String body = mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("id").asLong();
    }
}
