package br.com.candidaturas.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "integracao.token=segredo")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegracaoTokenIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void exigeTokenQuandoConfigurado() throws Exception {
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("X-Integracao-Token")));
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x").header("X-Integracao-Token", "errado"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x").header("X-Integracao-Token", "segredo"))
                .andExpect(status().isOk());
    }
}
