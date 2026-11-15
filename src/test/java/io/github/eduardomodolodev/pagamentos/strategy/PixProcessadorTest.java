package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PixProcessadorTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-15T14:00:00Z"), ZoneOffset.UTC);

    private final PixProcessador processador = new PixProcessador(RELOGIO);

    private static Conta conta(String saldo) {
        return new Conta("CONTA-001", "Maria", new BigDecimal(saldo), new BigDecimal("5000"));
    }

    private static Pagamento pix(String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal(valor))
                .destinatario("chave@pix.com")
                .build();
    }

    @Test
    void deveAprovarEDebitarOSaldo() {
        Conta conta = conta("1000.00");
        Pagamento pagamento = pix("200.00");

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.codigo()).isNull();
        assertThat(resultado.taxa()).isEqualByComparingTo("0");
        assertThat(resultado.total()).isEqualByComparingTo("200.00");
        assertThat(resultado.pagamentoId()).isEqualTo(pagamento.getId());
        assertThat(resultado.processadoEm()).isEqualTo(LocalDateTime.of(2026, 11, 15, 14, 0));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(conta.getSaldo()).isEqualByComparingTo("800.00");
    }

    @Test
    void naoDeveMexerNoLimiteDoCartao() {
        Conta conta = conta("1000.00");

        processador.processar(pix("200.00"), conta);

        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("5000.00");
    }

    @Test
    void deveDebitarValorMaisTaxaQuandoHouverTaxa() {
        Conta conta = conta("1000.00");
        Pagamento pagamento = pix("200.00");
        pagamento.adicionarTaxa(new BigDecimal("1.50"));

        processador.processar(pagamento, conta);

        assertThat(conta.getSaldo()).isEqualByComparingTo("798.50");
    }

    @Test
    void deveRecusarNaVerificacaoFinalQuandoSaldoForInsuficiente() {
        Conta conta = conta("100.00");
        Pagamento pagamento = pix("100.01");

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(conta.getSaldo()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveRecusarQuandoTaxaFazOTotalPassarDoSaldo() {
        Conta conta = conta("100.00");
        Pagamento pagamento = pix("100.00");
        pagamento.adicionarTaxa(new BigDecimal("0.01"));

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
        assertThat(conta.getSaldo()).isEqualByComparingTo("100.00");
    }
}
