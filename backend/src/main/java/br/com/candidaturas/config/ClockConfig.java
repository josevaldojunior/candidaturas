package br.com.candidaturas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Relógio único da aplicação; os services usam este bean para que os testes possam fixar a data. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.zona:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
