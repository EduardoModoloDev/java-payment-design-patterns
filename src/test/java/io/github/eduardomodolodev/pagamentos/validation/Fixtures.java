package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Dados de apoio compartilhados pelos testes de validação. */
final class Fixtures {

    static final LocalDateTime MEIO_DIA = LocalDateTime.of(2026, 11, 16, 12, 0);

    private Fixtures() {
    }

    static Clock relogioEm(LocalDateTime momento) {
        return Clock.fixed(momento.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    static Conta conta(String saldo, String limiteCartao) {
        return new Conta("CONTA-001", "Maria", new BigDecimal(saldo), new BigDecimal(limiteCartao));
    }

    static Pagamento.Builder pagamento(TipoPagamento tipo, String valor) {
        return pagamento(tipo, valor, MEIO_DIA);
    }

    static Pagamento.Builder pagamento(TipoPagamento tipo, String valor, LocalDateTime criadoEm) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .destinatario("Favorecido")
                .clock(relogioEm(criadoEm));
    }
}
