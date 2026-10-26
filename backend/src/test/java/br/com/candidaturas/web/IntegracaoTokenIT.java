package br.com.candidaturas.web;

import br.com.candidaturas.domain.Usuario;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.UsuarioRepository;
import br.com.candidaturas.seguranca.TokenIntegracao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O agente se autentica só pelo X-Integracao-Token, e os dados vão para a conta dona do token. */
@SpringBootTest
@ActiveProfiles("test")
class IntegracaoTokenIT {

    private static final String TOKEN = "cand_token-de-teste-da-integracao";

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CandidaturaRepository candidaturaRepository;

    private MockMvc mvc;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build(); // sem login padrão
        dono = new Usuario();
        dono.setNome("Dono do agente");
        dono.setEmail("agente@exemplo.com");
        dono.setSenhaHash("nao-usado");
        dono.setCriadoEm(LocalDateTime.of(2026, 10, 1, 9, 0));
        dono.setTokenIntegracaoHash(TokenIntegracao.hash(TOKEN));
        usuarioRepository.save(dono);
    }

    @AfterEach
    void limpar() {
        usuarioRepository.delete(dono); // apaga em cascata as candidaturas dele
    }

    @Test
    void semTokenOuComTokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem", containsString("X-Integracao-Token")));
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x").header("X-Integracao-Token", "errado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem", containsString("inválido")));
    }

    @Test
    void tokenValidoGravaNaContaDoDono() throws Exception {
        mvc.perform(post("/api/integracao/candidaturas").header("X-Integracao-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"chaveExterna\":\"tok-1\",\"empresa\":\"ACME\",\"cargo\":\"Dev Java\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criadas").value(1));

        assertThat(candidaturaRepository.findByUsuarioIdAndChaveExterna(dono.getId(), "tok-1")).isPresent();
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "tok-1").header("X-Integracao-Token", TOKEN))
                .andExpect(jsonPath("$.existe").value(true));
    }

    @Test
    void tokenDoAgenteNaoAbreAsOutrasRotas() throws Exception {
        mvc.perform(get("/api/candidaturas").header("X-Integracao-Token", TOKEN))
                .andExpect(status().isUnauthorized());
    }
}
