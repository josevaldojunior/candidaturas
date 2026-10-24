package br.com.candidaturas.web;

import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem @Transactional de propósito: a integração grava cada item numa transação própria,
 * então os dados são commitados de verdade e limpos ao fim de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
class IntegracaoControllerIT {

    private static final String ITEM = """
            [{"chaveExterna":"li-123","empresa":"ACME","cargo":"Desenvolvedor Java","linkVaga":"https://vagas/li-123",
              "plataforma":"linkedin","modalidade":"Remoto","tipoContrato":"PJ","nota":"8","dataCandidatura":"2026-10-20",
              "status":"Candidatado","prioridade":"Alta","stack":["Java","Spring Boot"],
              "contatoNome":"Ana","contatoEmail":"ana@acme.com","tipoInteracao":"e-mail",
              "descricaoInteracao":"Candidatura enviada","dataFollowUp":"2026-10-27"}]
            """;

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
    void criaERepetidoNaoDuplica() throws Exception {
        enviar(ITEM).andExpect(status().isOk())
                .andExpect(jsonPath("$.criadas").value(1))
                .andExpect(jsonPath("$.atualizadas").value(0))
                .andExpect(jsonPath("$.erros", hasSize(0)));

        enviar(ITEM).andExpect(jsonPath("$.criadas").value(0))
                .andExpect(jsonPath("$.ignoradas").value(1));

        enviar("""
                [{"chaveExterna":"li-123","status":"Triagem RH","descricaoInteracao":"RH chamou no LinkedIn","tipoInteracao":"mensagem"}]
                """).andExpect(jsonPath("$.atualizadas").value(1));

        mvc.perform(get("/api/candidaturas")).andExpect(jsonPath("$.totalElements").value(1));
        long id = candidaturaRepository.findByChaveExterna("li-123").orElseThrow().getId();
        mvc.perform(get("/api/candidaturas/{id}", id))
                .andExpect(jsonPath("$.origem").value("AGENTE"))
                .andExpect(jsonPath("$.status").value("TRIAGEM_RH"))
                .andExpect(jsonPath("$.empresa").value("ACME"))
                .andExpect(jsonPath("$.historico", hasSize(2)))
                .andExpect(jsonPath("$.contatos", hasSize(1)))
                // e-mail inicial + mudança de status + mensagem
                .andExpect(jsonPath("$.interacoes", hasSize(3)));
    }

    @Test
    void erroEmUmItemNaoAfetaOsDemais() throws Exception {
        enviar("""
                [{"empresa":"Beta","cargo":"Java","linkVaga":"https://vagas/beta"},
                 {"cargo":"Sem empresa","linkVaga":"https://vagas/x"},
                 {"empresa":"Gama","cargo":"Java","modalidade":"Marte"}]
                """)
                .andExpect(jsonPath("$.criadas").value(1))
                .andExpect(jsonPath("$.erros", contains("Item 2: empresa é obrigatória",
                        "Item 3: valor inválido para modalidade: 'Marte'")));
    }

    @Test
    void existePorLinkOuEmpresaECargo() throws Exception {
        enviar(ITEM);

        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "https://vagas/li-123"))
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.candidatura.empresa").value("ACME"))
                .andExpect(jsonPath("$.candidatura.status").value("CANDIDATADO"));
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "li-123"))
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/api/integracao/candidaturas/existe").param("empresa", "acme").param("cargo", "DESENVOLVEDOR JAVA"))
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/api/integracao/candidaturas/existe").param("link", "https://outra"))
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.candidatura").value(nullValue()));
    }

    @Test
    void aceitaObjetoUnicoEStackComoTexto() throws Exception {
        enviar("""
                {"empresa":"Delta","cargo":"Java","linkVaga":"https://vagas/delta","stack":"Java;Quarkus"}
                """).andExpect(jsonPath("$.criadas").value(1));

        mvc.perform(get("/api/candidaturas").param("stack", "quarkus"))
                .andExpect(jsonPath("$.content[0].empresa").value("Delta"));
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/api/integracao/candidaturas").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
