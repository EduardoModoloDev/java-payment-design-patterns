package io.github.eduardomodolodev.pagamentos.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaTest {

    private Conta novaConta() {
        return new Conta("CONTA-001", "Maria", new BigDecimal("1000"), new BigDecimal("3000"));
    }

    @Test
    void deveUsarLimiteDiarioPadraoDeCincoMil() {
        assertThat(novaConta().getLimiteDiario()).isEqualByComparingTo("5000.00");
    }

    @Test
    void deveNormalizarValoresParaEscalaDois() {
        Conta conta = novaConta();

        assertThat(conta.getSaldo()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualTo(new BigDecimal("3000.00"));
    }

    @Test
    void deveDebitarSaldo() {
        Conta conta = novaConta();

        conta.debitarSaldo(new BigDecimal("200.50"));

        assertThat(conta.getSaldo()).isEqualByComparingTo("799.50");
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("3000.00");
    }

    @Test
    void devePermitirDebitarSaldoInteiro() {
        Conta conta = novaConta();

        conta.debitarSaldo(new BigDecimal("1000.00"));

        assertThat(conta.getSaldo()).isEqualByComparingTo("0");
    }

    @Test
    void deveRecusarDebitoMaiorQueOSaldo() {
        Conta conta = novaConta();

        assertThatThrownBy(() -> conta.debitarSaldo(new BigDecimal("1000.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(conta.getSaldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    void deveDebitarLimiteDoCartaoSemMexerNoSaldo() {
        Conta conta = novaConta();

        conta.debitarLimiteCartao(new BigDecimal("1284.00"));

        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("1716.00");
        assertThat(conta.getSaldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    void deveRecusarDebitoMaiorQueOLimiteDoCartao() {
        Conta conta = novaConta();

        assertThatThrownBy(() -> conta.debitarLimiteCartao(new BigDecimal("3000.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Limite do cartão insuficiente");
    }

    @Test
    void deveRecusarDebitoNaoPositivo() {
        Conta conta = novaConta();

        assertThatThrownBy(() -> conta.debitarSaldo(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conta.debitarLimiteCartao(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveExigirIdETitular() {
        assertThatThrownBy(() -> new Conta(" ", "Maria", BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Conta("CONTA-001", null, BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naoDeveAceitarSaldoNegativo() {
        assertThatThrownBy(() -> new Conta("CONTA-001", "Maria", new BigDecimal("-1"), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
