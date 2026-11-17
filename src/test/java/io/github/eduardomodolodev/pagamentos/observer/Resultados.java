package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamentos e resultados prontos para os testes dos listeners. */
final class Resultados {

    static final LocalDateTime PROCESSADO_EM = LocalDateTime.of(2026, 11, 17, 10, 30);

    private Resultados() {
    }

    static Pagamento pix(String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal(valor))
                .destinatario("chave@pix.com")
                .build();
    }

    static ResultadoPagamento finalizar(Pagamento pagamento, StatusPagamento status, String codigo) {
        pagamento.atualizarStatus(status);
        return ResultadoPagamento.de(pagamento, codigo, "mensagem de teste", PROCESSADO_EM);
    }
}
