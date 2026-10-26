package br.com.candidaturas.web;

import br.com.candidaturas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de conta: cadastro, login com JWT de verdade, isolamento entre contas e token do agente.
 * Usa o relógio real (sem RelogioFixo): o JWT é validado contra a hora atual.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContaControllerIT {

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build(); // sem login padrão
    }

    @AfterEach
    void limpar() {
        usuarioRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@conta.test"))
                .forEach(usuarioRepository::delete);
    }

    @Test
    void rotasProtegidasExigemLoginEAsPublicasNao() throws Exception {
        mvc.perform(get("/api/candidaturas")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem", containsString("login")));
        mvc.perform(get("/api/candidaturas").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/config")).andExpect(status().isOk()).andExpect(jsonPath("$.cadastroAberto").value(true));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void cadastroLoginEDadosDaConta() throws Exception {
        String token = cadastrar("Ana", "Ana@Conta.Test", "senha-forte-1");

        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@conta.test"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ANA@conta.test\",\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.usuario.nome").value("Ana"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@conta.test\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@conta.test\",\"senha\":\"qualquer-uma\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void cadastroValidaDadosEEmailRepetido() throws Exception {
        cadastrar("Bia", "bia@conta.test", "senha-forte-1");

        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra\",\"email\":\"BIA@conta.test\",\"senha\":\"senha-forte-2\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("e-mail")));
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", containsInAnyOrder("nome", "email", "senha")));
    }

    @Test
    void cadaContaSoEnxergaOsProprioDados() throws Exception {
        String ana = cadastrar("Ana", "ana2@conta.test", "senha-forte-1");
        String beto = cadastrar("Beto", "beto@conta.test", "senha-forte-1");

        String criada = mvc.perform(post("/api/candidaturas").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"Só da Ana\",\"cargo\":\"Dev Java\",\"chaveExterna\":\"mesma-chave\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(criada).get("id").asLong();

        mvc.perform(get("/api/candidaturas").header("Authorization", "Bearer " + beto))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/candidaturas/{id}", id).header("Authorization", "Bearer " + beto))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/candidaturas/{id}", id).header("Authorization", "Bearer " + beto))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + beto))
                .andExpect(jsonPath("$.total").value(0));

        // a chave externa é única por conta: o Beto pode usar a mesma
        mvc.perform(post("/api/candidaturas").header("Authorization", "Bearer " + beto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"Do Beto\",\"cargo\":\"Dev Java\",\"chaveExterna\":\"mesma-chave\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/candidaturas").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.content[*].empresa", contains("Só da Ana")));
    }

    @Test
    void tokenDeIntegracaoGeradoPelaContaAlimentaSoElaETrocaSenha() throws Exception {
        String sessao = cadastrar("Caio", "caio@conta.test", "senha-forte-1");

        mvc.perform(get("/api/conta/token-integracao").header("Authorization", "Bearer " + sessao))
                .andExpect(jsonPath("$.configurado").value(false));
        String gerado = mvc.perform(post("/api/conta/token-integracao").header("Authorization", "Bearer " + sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", startsWith("cand_")))
                .andReturn().getResponse().getContentAsString();
        String tokenAgente = mapper.readTree(gerado).get("token").asText();
        assertThat(usuarioRepository.findByEmail("caio@conta.test").orElseThrow().getTokenIntegracaoHash())
                .isNotEqualTo(tokenAgente); // no banco fica só o hash

        mvc.perform(post("/api/integracao/candidaturas").header("X-Integracao-Token", tokenAgente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"empresa\":\"Via agente\",\"cargo\":\"Dev Java\",\"linkVaga\":\"https://vagas/caio\"}]"))
                .andExpect(jsonPath("$.criadas").value(1));
        mvc.perform(get("/api/candidaturas").header("Authorization", "Bearer " + sessao))
                .andExpect(jsonPath("$.content[0].empresa").value("Via agente"))
                .andExpect(jsonPath("$.content[0].origem").value("AGENTE"));

        mvc.perform(delete("/api/conta/token-integracao").header("Authorization", "Bearer " + sessao))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "x").header("X-Integracao-Token", tokenAgente))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/conta/senha").header("Authorization", "Bearer " + sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"nova-senha-1\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/conta/senha").header("Authorization", "Bearer " + sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-forte-1\",\"novaSenha\":\"nova-senha-1\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"caio@conta.test\",\"senha\":\"nova-senha-1\"}"))
                .andExpect(status().isOk());
    }

    private String cadastrar(String nome, String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("token").asText();
    }
}
