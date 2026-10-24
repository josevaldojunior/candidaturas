package br.com.candidaturas.web;

import br.com.candidaturas.support.RelogioFixo;
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
class FollowUpESemRetornoIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void listaEConcluiFollowUps() throws Exception {
        long id = criar("{\"empresa\":\"ACME\",\"cargo\":\"Java\"}");
        long vencido = interacao(id, "2026-10-20");
        interacao(id, "2026-10-27");
        interacao(id, "2026-11-15"); // fora da janela de 7 dias

        mvc.perform(get("/api/follow-ups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencidos", hasSize(1)))
                .andExpect(jsonPath("$.vencidos[0].empresa").value("ACME"))
                .andExpect(jsonPath("$.vencidos[0].diasParaVencer").value(-4))
                .andExpect(jsonPath("$.proximos", hasSize(1)))
                .andExpect(jsonPath("$.proximos[0].dataFollowUp").value("2026-10-27"));

        mvc.perform(patch("/api/interacoes/{id}/follow-up", vencido)).andExpect(status().isOk());

        mvc.perform(get("/api/follow-ups")).andExpect(jsonPath("$.vencidos", hasSize(0)));
    }

    @Test
    void configuraEExecutaSemRetorno() throws Exception {
        mvc.perform(get("/api/sem-retorno/configuracao")).andExpect(jsonPath("$.dias").value(21));

        long antiga = criar("{\"empresa\":\"Antiga\",\"cargo\":\"Java\",\"dataCandidatura\":\"2026-09-01\"}");
        long recente = criar("{\"empresa\":\"Recente\",\"cargo\":\"Java\",\"dataCandidatura\":\"2026-10-20\"}");
        long interesse = criar("{\"empresa\":\"Interesse\",\"cargo\":\"Java\",\"status\":\"INTERESSE\",\"dataCandidatura\":\"2026-08-01\"}");

        mvc.perform(post("/api/sem-retorno/executar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcadas").value(1))
                .andExpect(jsonPath("$.ids", contains((int) antiga)));

        mvc.perform(get("/api/candidaturas/{id}", antiga))
                .andExpect(jsonPath("$.status").value("SEM_RETORNO"))
                .andExpect(jsonPath("$.historico[-1:].statusNovo", contains("SEM_RETORNO")))
                .andExpect(jsonPath("$.interacoes[0].tipo").value("MUDANCA_STATUS"));
        mvc.perform(get("/api/candidaturas/{id}", recente)).andExpect(jsonPath("$.status").value("CANDIDATADO"));
        mvc.perform(get("/api/candidaturas/{id}", interesse)).andExpect(jsonPath("$.status").value("INTERESSE"));

        mvc.perform(put("/api/sem-retorno/configuracao").contentType(MediaType.APPLICATION_JSON).content("{\"dias\":3}"))
                .andExpect(jsonPath("$.dias").value(3));
        mvc.perform(post("/api/sem-retorno/executar")).andExpect(jsonPath("$.ids", contains((int) recente)));

        mvc.perform(put("/api/sem-retorno/configuracao").contentType(MediaType.APPLICATION_JSON).content("{\"dias\":0}"))
                .andExpect(status().isBadRequest());
    }

    private long criar(String json) throws Exception {
        String body = mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private long interacao(long candidaturaId, String followUp) throws Exception {
        String body = mvc.perform(post("/api/candidaturas/{id}/interacoes", candidaturaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMAIL\",\"descricao\":\"cobrar\",\"dataFollowUp\":\"" + followUp + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
}
