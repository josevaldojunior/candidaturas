package br.com.candidaturas.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio fixo em 24/10/2026 10:00 (America/Sao_Paulo) para testes determinísticos. */
@TestConfiguration
public class RelogioFixo {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    public static final Instant AGORA = Instant.parse("2026-10-24T13:00:00Z");

    public static Clock clock() {
        return Clock.fixed(AGORA, ZONA);
    }

    @Bean
    @Primary
    public Clock relogioFixo() {
        return clock();
    }
}
