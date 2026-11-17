package io.github.eduardomodolodev.pagamentos.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Fixa o relógio da API em 17/11/2026 às 14h para os testes não dependerem da hora real. */
@TestConfiguration(proxyBeanMethods = false)
class RelogioFixoConfig {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 17, 14, 0);

    @Bean
    @Primary
    Clock relogioFixo() {
        return Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }
}
