package br.com.candidaturas.web;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PlataformaControllerIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired br.com.candidaturas.repository.CandidaturaRepository candidaturas;
    private String payload(String nome,int ordem,boolean ativa) throws Exception {
        return mapper.writeValueAsString(java.util.Map.of("nome",nome,"ordem",ordem,"ativa",ativa));
    }
    private JsonNode criar(String nome) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/plataformas").contentType(MediaType.APPLICATION_JSON)
            .content(payload(nome,3,true))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void cadastroCompletoComNomeSemAcentoCodigoEstavelEOrdem() throws Exception {
        var lista=mapper.readTree(mvc.perform(get("/api/plataformas")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(lista).hasSize(13);
        assertThat(lista.findValuesAsText("nome")).contains("Sólides");
        var criada=criar("Vagas da Região"); long id=criada.get("id").asLong(); String codigo=criada.get("codigo").asText();
        mvc.perform(put("/api/plataformas/{id}",id).contentType(MediaType.APPLICATION_JSON)
            .content(payload("Regional",0,true))).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value(codigo));
        mvc.perform(get("/api/plataformas/{id}",id)).andExpect(jsonPath("$.nome").value("Regional"));
        mvc.perform(post("/api/plataformas").contentType(MediaType.APPLICATION_JSON).content(payload(" regional ",4,true)))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/plataformas/{id}",id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/plataformas/{id}",id)).andExpect(status().isNotFound());
    }
    @Test void outraContaNaoPodeLerAlterarOuExcluir() throws Exception {
        long id=criar("Só da minha conta").get("id").asLong();
        mvc.perform(get("/api/plataformas/{id}",id).with(jwt().jwt(j->j.subject("9999")))).andExpect(status().isNotFound());
        mvc.perform(put("/api/plataformas/{id}",id).with(jwt().jwt(j->j.subject("9999")))
            .contentType(MediaType.APPLICATION_JSON).content(payload("Alterada",1,true))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/plataformas/{id}",id).with(jwt().jwt(j->j.subject("9999")))).andExpect(status().isNotFound());
    }
    @Test void plataformaEmUsoPodeSerDesativadaMasNaoExcluida() throws Exception {
        var p=criar("Minha fonte"); long id=p.get("id").asLong(); String codigo=p.get("codigo").asText();
        String vaga=mapper.writeValueAsString(java.util.Map.of("empresa","Acme","cargo","Dev","plataforma",codigo));
        long candidatura=mapper.readTree(mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(vaga))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/plataformas/{id}",id)).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/plataformas/{id}",id).contentType(MediaType.APPLICATION_JSON).content(payload("Minha fonte",3,false)))
            .andExpect(status().isOk());
        mvc.perform(put("/api/candidaturas/{id}",candidatura).contentType(MediaType.APPLICATION_JSON).content(vaga)).andExpect(status().isOk());
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(vaga)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/candidaturas").param("plataforma",codigo)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"empresa\":\"Acme\",\"cargo\":\"Dev\",\"plataforma\":\"INEXISTENTE\"}")).andExpect(status().isUnprocessableEntity());
    }
    @Test @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED) void agenteReconheceSolidesNomePersonalizadoECsv() throws Exception {
        var p=criar("Portal São João"); String codigo=p.get("codigo").asText();
        try {
        mvc.perform(post("/api/integracao/candidaturas").contentType(MediaType.APPLICATION_JSON)
            .content("[{\"empresa\":\"Acme\",\"cargo\":\"Dev\",\"chaveExterna\":\"s-1\",\"plataforma\":\"Sólides\"},"
                +"{\"empresa\":\"Acme\",\"cargo\":\"Dev\",\"chaveExterna\":\"s-2\",\"plataforma\":\"portal sao joao\"}]"))
            .andExpect(jsonPath("$.criadas").value(2)).andExpect(jsonPath("$.erros.length()").value(0));
        mvc.perform(get("/api/candidaturas").param("plataforma","SOLIDES")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/candidaturas").param("plataforma",codigo)).andExpect(jsonPath("$.totalElements").value(1));
        String csv=mvc.perform(get("/api/csv/exportar").param("plataforma",codigo)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(csv).contains(codigo);
        mvc.perform(put("/api/plataformas/{id}",p.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("Outro nome",3,true))).andExpect(status().isOk());
        mvc.perform(post("/api/integracao/candidaturas").contentType(MediaType.APPLICATION_JSON)
            .content("[{\"empresa\":\"Acme\",\"cargo\":\"Dev\",\"chaveExterna\":\"s-3\",\"plataforma\":\""+codigo+"\"}]"))
            .andExpect(jsonPath("$.criadas").value(1));
        } finally {
            for (var c : candidaturas.findAll()) {
                if (c.getChaveExterna() != null && c.getChaveExterna().startsWith("s-")) candidaturas.deleteById(c.getId());
            }
            mvc.perform(delete("/api/plataformas/{id}",p.get("id").asLong())).andExpect(status().isNoContent());
        }
    }
    @Test void protegeOutrosEValidaCampos() throws Exception {
        JsonNode lista=mapper.readTree(mvc.perform(get("/api/plataformas")).andReturn().getResponse().getContentAsString());
        long id=java.util.stream.StreamSupport.stream(lista.spliterator(),false).filter(p->p.get("codigo").asText().equals("OUTRO"))
            .findFirst().orElseThrow().get("id").asLong();
        mvc.perform(delete("/api/plataformas/{id}",id)).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/plataformas/{id}",id).contentType(MediaType.APPLICATION_JSON).content(payload("Outros",12,false))).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/plataformas").contentType(MediaType.APPLICATION_JSON).content(payload("",-1,true))).andExpect(status().isBadRequest());
    }
}
