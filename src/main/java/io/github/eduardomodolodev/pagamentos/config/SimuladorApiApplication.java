package io.github.eduardomodolodev.pagamentos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API REST (fase 4).
 *
 * <p>O Spring fica restrito às camadas {@code config} e {@code api}: o núcleo
 * (domain, strategy, decorator, factory, validation, observer, service)
 * continua Java puro, sem nenhuma anotação do framework.
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.eduardomodolodev.pagamentos.config",
        "io.github.eduardomodolodev.pagamentos.api"
})
public class SimuladorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuladorApiApplication.class, args);
    }
}
