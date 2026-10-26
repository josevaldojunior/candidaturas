package br.com.candidaturas.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CADASTRO_ABERTO=false: com uma conta já existente (a de teste), ninguém mais se cadastra. */
@SpringBootTest(properties = "app.seguranca.cadastro-aberto=false")
@ActiveProfiles("test")
class CadastroFechadoIT {

    @Autowired
    private WebApplicationContext contexto;

    @Test
    void naoAceitaNovasContas() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        mvc.perform(get("/api/auth/config")).andExpect(jsonPath("$.cadastroAberto").value(false));
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Intrusa\",\"email\":\"intrusa@exemplo.com\",\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("O cadastro de novas contas está fechado"));
    }
}
