package br.com.candidaturas.support;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Nos testes de integração, toda requisição do MockMvc sai logada como o usuário de teste
 * (id 1000, criado em db/testdata). Testes de login/segurança montam um MockMvc próprio, sem isso.
 */
@Configuration
public class LoginPadraoNosTestes {

    public static final long USUARIO_TESTE = 1000L;

    @Bean
    MockMvcBuilderCustomizer loginPadrao() {
        return builder -> builder.defaultRequest(get("/").with(jwt().jwt(j -> j.subject(String.valueOf(USUARIO_TESTE)))));
    }
}
