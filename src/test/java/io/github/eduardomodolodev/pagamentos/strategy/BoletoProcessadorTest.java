package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BoletoProcessadorTest {

    private final BoletoProcessador processador = new BoletoProcessador();

    @Test
    void deveDeixarPendenteSemDebitarNada() {
        Conta conta = new Conta("CONTA-001", "Maria", new BigDecimal("100.00"), new BigDecimal("100.00"));
        Pagamento pagamento = Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.BOLETO)
                .valor(new BigDecimal("350.00"))
                .destinatario("Companhia de Energia")
                .dataVencimento(LocalDate.of(2026, 12, 10))
                .build();

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(resultado.codigo()).isNull();
        assertThat(resultado.mensagem()).contains("2026-12-10");
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(conta.getSaldo()).isEqualByComparingTo("100.00");
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("100.00");
    }
}
