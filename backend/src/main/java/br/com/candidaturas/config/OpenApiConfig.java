package br.com.candidaturas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentação da API. O botão "Authorize" do Swagger aceita o login (Bearer) ou o token de integração. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Candidaturas API")
                        .version("1.0.0")
                        .description("Acompanhamento de candidaturas a vagas: contas, CRUD, pipeline de status, "
                                + "interações, contatos, dashboard e integração com agentes/automações. "
                                + "Faça login em POST /api/auth/login e use o token no botão Authorize.")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("login", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token devolvido por /api/auth/login ou /api/auth/cadastro"))
                        .addSecuritySchemes("tokenIntegracao", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Integracao-Token")
                                .description("Token da conta para agentes e automações (Configurações → Integração)")))
                .addSecurityItem(new SecurityRequirement().addList("login"));
    }
}
