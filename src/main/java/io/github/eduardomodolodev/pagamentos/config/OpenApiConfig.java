package io.github.eduardomodolodev.pagamentos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentação OpenAPI exibida no Swagger UI ({@code /swagger-ui.html}). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI simuladorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Simulador de Pagamentos")
                .version("1.1.0")
                .description("""
                        API REST do simulador de pagamentos (Pix, cartão de crédito e boleto) construído \
                        com padrões de projeto: Strategy, Factory Method, Chain of Responsibility, \
                        Decorator, Observer, Builder e Facade.

                        Pagamentos recusados por regra de negócio respondem **201** com `status = RECUSADO` \
                        e o motivo em `codigo`. Erros HTTP ficam para requisição inválida (400), recurso \
                        inexistente (404) e conflito de estado (409).

                        Contas de demonstração: `CONTA-001`, `CONTA-002` e `CONTA-003`.""")
                .contact(new Contact()
                        .name("Eduardo Modolo")
                        .url("https://github.com/EduardoModoloDev/java-payment-design-patterns")));
    }
}
