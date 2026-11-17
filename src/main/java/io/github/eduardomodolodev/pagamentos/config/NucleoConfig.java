package io.github.eduardomodolodev.pagamentos.config;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.observer.AuditoriaListener;
import io.github.eduardomodolodev.pagamentos.observer.EmailListener;
import io.github.eduardomodolodev.pagamentos.observer.PagamentoListener;
import io.github.eduardomodolodev.pagamentos.observer.SmsListener;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import io.github.eduardomodolodev.pagamentos.validation.CadeiaValidadores;
import io.github.eduardomodolodev.pagamentos.validation.Validador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * Monta os objetos do núcleo como beans do Spring.
 *
 * <p>É o mesmo "encanamento" que o {@code Main} faz à mão: o núcleo não sabe
 * que existe Spring. Trocar os repositórios em memória por JPA, por exemplo,
 * seria mudar só os beans abaixo.
 */
@Configuration(proxyBeanMethods = false)
public class NucleoConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Repositório de contas já com as contas de demonstração. */
    @Bean
    ContaRepository contaRepository() {
        ContaRepository contas = new ContaRepositoryEmMemoria();
        contas.salvar(new Conta("CONTA-001", "Maria Silva", new BigDecimal("3000.00"), new BigDecimal("5000.00")));
        contas.salvar(new Conta("CONTA-002", "João Souza", new BigDecimal("800.00"), new BigDecimal("2000.00")));
        contas.salvar(new Conta("CONTA-003", "Ana Lima", new BigDecimal("10000.00"), new BigDecimal("5000.00")));
        return contas;
    }

    @Bean
    PagamentoRepository pagamentoRepository() {
        return new PagamentoRepositoryEmMemoria();
    }

    @Bean
    ProcessadorFactory processadorFactory(Clock clock) {
        return new ProcessadorFactory(clock);
    }

    @Bean
    Validador cadeiaValidacoes(PagamentoRepository pagamentoRepository, Clock clock) {
        return CadeiaValidadores.padrao(pagamentoRepository, clock);
    }

    @Bean
    EmailListener emailListener() {
        return new EmailListener();
    }

    @Bean
    SmsListener smsListener() {
        return new SmsListener();
    }

    @Bean
    AuditoriaListener auditoriaListener() {
        return new AuditoriaListener();
    }

    /** A Facade, com todos os listeners registrados. */
    @Bean
    PagamentoService pagamentoService(ContaRepository contaRepository,
                                      PagamentoRepository pagamentoRepository,
                                      ProcessadorFactory processadorFactory,
                                      Validador cadeiaValidacoes,
                                      Clock clock,
                                      List<PagamentoListener> listeners) {
        PagamentoService service = new PagamentoService(
                contaRepository, pagamentoRepository, processadorFactory, cadeiaValidacoes, clock);
        listeners.forEach(service::registrarListener);
        return service;
    }
}
