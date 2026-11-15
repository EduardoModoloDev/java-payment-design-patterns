package io.github.eduardomodolodev.pagamentos;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Demonstração do simulador em Java puro.
 *
 * <p>Fase 1: cenários 1 (Pix), 3 (cartão) e 5 (boleto) da especificação.
 * Taxas, juros e cashback entram na fase 2, com os decorators.
 */
public class Main {

    private static final NumberFormat REAIS = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();

        // Montagem manual das dependências (sem Singleton, sem framework).
        ContaRepository contas = new ContaRepositoryEmMemoria();
        PagamentoService service = new PagamentoService(
                contas, new PagamentoRepositoryEmMemoria(), new ProcessadorFactory(clock));

        Conta maria = contas.salvar(new Conta("CONTA-001", "Maria Silva",
                new BigDecimal("3000.00"), new BigDecimal("5000.00")));
        contas.salvar(new Conta("CONTA-002", "João Souza",
                new BigDecimal("800.00"), new BigDecimal("2000.00")));

        titulo("Simulador de Pagamentos — Fase 1 (Strategy + Factory + Builder + Facade)");
        System.out.println("Conta inicial: " + descrever(maria));

        executar("Cenário 1 — Pix de R$ 200,00 com saldo suficiente", service,
                Pagamento.builder()
                        .contaOrigemId("CONTA-001")
                        .tipo(TipoPagamento.PIX)
                        .valor(new BigDecimal("200.00"))
                        .destinatario("joao@pix.com")
                        .clock(clock)
                        .build());

        executar("Cenário 3 — Cartão de R$ 1.200,00 em 6x", service,
                Pagamento.builder()
                        .contaOrigemId("CONTA-001")
                        .tipo(TipoPagamento.CARTAO_CREDITO)
                        .valor(new BigDecimal("1200.00"))
                        .destinatario("Loja do Zé")
                        .parcelas(6)
                        .clock(clock)
                        .build());

        executar("Cenário 5 — Boleto de R$ 350,00 com vencimento futuro", service,
                Pagamento.builder()
                        .contaOrigemId("CONTA-001")
                        .tipo(TipoPagamento.BOLETO)
                        .valor(new BigDecimal("350.00"))
                        .destinatario("Companhia de Energia")
                        .dataVencimento(LocalDate.now(clock).plusDays(10))
                        .clock(clock)
                        .build());

        titulo("Situação final");
        System.out.println(descrever(maria));
        System.out.println();
        System.out.println("Histórico da CONTA-001:");
        service.listarPagamentos("CONTA-001").forEach(pagamento ->
                System.out.printf("  - %-15s %12s  %-9s %s%n",
                        pagamento.getTipo(),
                        REAIS.format(pagamento.getValor()),
                        pagamento.getStatus(),
                        pagamento.getDestinatario()));
    }

    private static void executar(String cenario, PagamentoService service, Pagamento pagamento) {
        titulo(cenario);
        ResultadoPagamento resultado = service.processar(pagamento);
        System.out.println("Status ....: " + resultado.status()
                + (resultado.codigo() == null ? "" : " (" + resultado.codigo() + ")"));
        System.out.println("Mensagem ..: " + resultado.mensagem());
        System.out.println("Valor .....: " + REAIS.format(resultado.valor()));
        System.out.println("Taxa ......: " + REAIS.format(resultado.taxa()));
        System.out.println("Total .....: " + REAIS.format(resultado.total()));
        System.out.println("Cashback ..: " + REAIS.format(resultado.cashback()));
    }

    private static String descrever(Conta conta) {
        return "%s (%s) — saldo %s, limite do cartão %s".formatted(
                conta.getId(), conta.getTitular(),
                REAIS.format(conta.getSaldo()), REAIS.format(conta.getLimiteCartaoDisponivel()));
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}
