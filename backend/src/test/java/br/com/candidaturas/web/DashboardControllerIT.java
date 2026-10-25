package br.com.candidaturas.web;

import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
@Transactional
class DashboardControllerIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void resumoUsaHistoricoGravadoNoBanco() throws Exception {
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"empresa\":\"ACME\",\"cargo\":\"Java\",\"plataforma\":\"LINKEDIN_VAGAS\"}"));
        String body = mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"Beta\",\"cargo\":\"Java\",\"plataforma\":\"GUPY\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*?\"id\":(\\d+).*", "$1");
        mvc.perform(patch("/api/candidaturas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ENTREVISTA_TECNICA\"}"));
        // voltar o status não apaga a etapa alcançada no funil
        mvc.perform(patch("/api/candidaturas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RECUSADA\"}"));

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.comResposta").value(1))
                .andExpect(jsonPath("$.taxaResposta").value(50.0))
                .andExpect(jsonPath("$.funil", hasSize(8)))
                .andExpect(jsonPath("$.funil[4].etapa").value("ENTREVISTA_TECNICA"))
                .andExpect(jsonPath("$.funil[4].quantidade").value(1))
                .andExpect(jsonPath("$.porSemana", hasSize(12)))
                .andExpect(jsonPath("$.porSemana[11].quantidade").value(2));
    }
}
