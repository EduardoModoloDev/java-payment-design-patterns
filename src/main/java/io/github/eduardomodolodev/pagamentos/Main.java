package io.github.eduardomodolodev.pagamentos;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.observer.AuditoriaListener;
import io.github.eduardomodolodev.pagamentos.observer.EmailListener;
import io.github.eduardomodolodev.pagamentos.observer.SmsListener;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import io.github.eduardomodolodev.pagamentos.validation.CadeiaValidadores;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Demonstração do simulador em Java puro.
 *
 * <p>Roda os 10 cenários da especificação, com notificações de e-mail, SMS e
 * auditoria. Um relógio simulado controla o horário, para que os cenários não
 * dependam da hora real.
 */
public class Main {

    private static final NumberFormat REAIS = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    public static void main(String[] args) {
        ZoneId zona = ZoneId.systemDefault();
        RelogioSimulado relogio = new RelogioSimulado(LocalDateTime.of(2026, 11, 16, 14, 0), zona);
        Clock noite = Clock.fixed(LocalDateTime.of(2026, 11, 16, 22, 0).atZone(zona).toInstant(), zona);

        // Montagem manual das dependências (sem Singleton, sem framework).
        ContaRepository contas = new ContaRepositoryEmMemoria();
        PagamentoRepository pagamentos = new PagamentoRepositoryEmMemoria();
        PagamentoService service = novoService(contas, pagamentos, relogio);
        PagamentoService serviceNoturno = novoService(contas, pagamentos, noite);

        // Observer: os mesmos ouvintes nos dois services.
        AuditoriaListener auditoria = new AuditoriaListener();
        for (PagamentoService s : new PagamentoService[]{service, serviceNoturno}) {
            s.registrarListener(new EmailListener());
            s.registrarListener(new SmsListener());
            s.registrarListener(auditoria);
        }

        Conta maria = contas.salvar(new Conta("CONTA-001", "Maria Silva",
                new BigDecimal("3000.00"), new BigDecimal("5000.00")));
        Conta joao = contas.salvar(new Conta("CONTA-002", "João Souza",
                new BigDecimal("800.00"), new BigDecimal("2000.00")));
        Conta ana = contas.salvar(new Conta("CONTA-003", "Ana Lima",
                new BigDecimal("10000.00"), new BigDecimal("5000.00")));
        Conta carlos = contas.salvar(new Conta("CONTA-004", "Carlos Melo",
                new BigDecimal("10000.00"), new BigDecimal("5000.00")));

        titulo("Simulador de Pagamentos — Strategy, Factory, Chain, Decorator, Observer, Builder e Facade");
        System.out.println("Contas iniciais:");
        for (Conta conta : new Conta[]{maria, joao, ana, carlos}) {
            System.out.println("  " + descrever(conta));
        }

        executar("Cenário 1 — Pix de R$ 200,00 com saldo suficiente", service,
                pix("CONTA-001", "200.00", "joao@pix.com", relogio));

        relogio.avancar(Duration.ofMinutes(1));
        executar("Cenário 2 — Pix de R$ 1.500,00 às 22h", serviceNoturno,
                pix("CONTA-002", "1500.00", "loja@pix.com", noite));

        relogio.avancar(Duration.ofMinutes(1));
        executar("Cenário 3 — Cartão de R$ 1.200,00 em 6x", service,
                cartao("CONTA-001", "1200.00", 6, relogio));

        relogio.avancar(Duration.ofMinutes(1));
        executar("Cenário 4 — Cartão de R$ 900,00 em 13x", service,
                cartao("CONTA-001", "900.00", 13, relogio));

        relogio.avancar(Duration.ofMinutes(1));
        ResultadoPagamento boletoPendente = executar("Cenário 5 — Boleto de R$ 350,00 com vencimento futuro",
                service, boleto("CONTA-001", "350.00", LocalDate.now(relogio).plusDays(10), relogio));

        relogio.avancar(Duration.ofMinutes(1));
        titulo("Cenário 6 — confirmarBoleto do cenário 5");
        imprimir(service.confirmarBoleto(boletoPendente.pagamentoId()));
        System.out.println("Saldo da CONTA-001 após o boleto: " + formatar(maria.getSaldo()));

        relogio.avancar(Duration.ofMinutes(1));
        executar("Cenário 7 — Boleto vencido", service,
                boleto("CONTA-001", "120.00", LocalDate.now(relogio).minusDays(1), relogio));

        relogio.avancar(Duration.ofMinutes(1));
        executar("Cenário 8 — Pix maior que o saldo", service,
                pix("CONTA-002", "1000.00", "amigo@pix.com", relogio));

        titulo("Cenário 9 — 5 pagamentos seguidos em poucos segundos (CONTA-003)");
        for (int i = 1; i <= 6; i++) {
            relogio.avancar(Duration.ofSeconds(10));
            ResultadoPagamento resultado = service.processar(
                    pix("CONTA-003", "50.00", "cafe@pix.com", relogio));
            System.out.printf("  %dº Pix de R$ 50,00 -> %s%n", i, resumir(resultado));
        }

        relogio.avancar(Duration.ofMinutes(5));
        titulo("Cenário 10 — Pagamentos acumulados passando de R$ 5.000,00 no dia (CONTA-004)");
        for (String valor : new String[]{"2000.00", "2000.00", "1500.00"}) {
            relogio.avancar(Duration.ofMinutes(1));
            ResultadoPagamento resultado = service.processar(
                    pix("CONTA-004", valor, "fornecedor@pix.com", relogio));
            System.out.printf("  Pix de %s -> %s%n", formatar(new BigDecimal(valor)), resumir(resultado));
        }

        titulo("Situação final das contas");
        for (Conta conta : new Conta[]{maria, joao, ana, carlos}) {
            System.out.println("  " + descrever(conta));
        }

        titulo("Auditoria");
        System.out.printf("%d eventos registrados:%n", auditoria.getEventos().size());
        auditoria.getEventos().forEach(evento -> System.out.printf("  %s  %-9s %-15s %10s  %-9s %s%n",
                evento.processadoEm().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME), evento.contaId(), evento.tipo(),
                evento.total(), evento.status(), evento.codigo() == null ? "" : evento.codigo()));
    }

    private static PagamentoService novoService(ContaRepository contas, PagamentoRepository pagamentos, Clock clock) {
        return new PagamentoService(contas, pagamentos, new ProcessadorFactory(clock),
                CadeiaValidadores.padrao(pagamentos, clock), clock);
    }

    private static Pagamento pix(String conta, String valor, String chave, Clock clock) {
        return Pagamento.builder()
                .contaOrigemId(conta)
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal(valor))
                .destinatario(chave)
                .clock(clock)
                .build();
    }

    private static Pagamento cartao(String conta, String valor, int parcelas, Clock clock) {
        return Pagamento.builder()
                .contaOrigemId(conta)
                .tipo(TipoPagamento.CARTAO_CREDITO)
                .valor(new BigDecimal(valor))
                .destinatario("Loja do Zé")
                .parcelas(parcelas)
                .clock(clock)
                .build();
    }

    private static Pagamento boleto(String conta, String valor, LocalDate vencimento, Clock clock) {
        return Pagamento.builder()
                .contaOrigemId(conta)
                .tipo(TipoPagamento.BOLETO)
                .valor(new BigDecimal(valor))
                .destinatario("Companhia de Energia")
                .dataVencimento(vencimento)
                .clock(clock)
                .build();
    }

    private static ResultadoPagamento executar(String cenario, PagamentoService service, Pagamento pagamento) {
        titulo(cenario);
        ResultadoPagamento resultado = service.processar(pagamento);
        imprimir(resultado);
        return resultado;
    }

    private static void imprimir(ResultadoPagamento resultado) {
        System.out.println("Status ....: " + resultado.status()
                + (resultado.codigo() == null ? "" : " (" + resultado.codigo() + ")"));
        System.out.println("Mensagem ..: " + resultado.mensagem());
        System.out.println("Valor .....: " + formatar(resultado.valor()));
        System.out.println("Taxa ......: " + formatar(resultado.taxa()));
        System.out.println("Total .....: " + formatar(resultado.total()));
        System.out.println("Cashback ..: " + formatar(resultado.cashback()));
    }

    private static String resumir(ResultadoPagamento resultado) {
        return resultado.codigo() == null
                ? resultado.status().name()
                : resultado.status() + " (" + resultado.codigo() + ")";
    }

    private static String descrever(Conta conta) {
        return "%s (%s): saldo %s, limite do cartão %s".formatted(
                conta.getId(), conta.getTitular(),
                formatar(conta.getSaldo()), formatar(conta.getLimiteCartaoDisponivel()));
    }

    /** Formata em reais trocando o espaço não separável por um espaço comum (melhor em qualquer console). */
    private static String formatar(BigDecimal valor) {
        return REAIS.format(valor).replace(' ', ' ');
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }

    /** Relógio controlável para a demonstração: começa num horário fixo e avança quando pedido. */
    private static final class RelogioSimulado extends Clock {

        private final ZoneId zona;
        private Instant agora;

        RelogioSimulado(LocalDateTime inicio, ZoneId zona) {
            this.zona = zona;
            this.agora = inicio.atZone(zona).toInstant();
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId novaZona) {
            return Clock.fixed(agora, novaZona);
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
