package io.github.eduardomodolodev.pagamentos.api.dto;

import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado de um processamento ou confirmação de boleto.
 * {@code codigo} é {@code null}, a não ser que o pagamento tenha sido recusado.
 */
public record ResultadoPagamentoResponse(
        String pagamentoId,
        StatusPagamento status,
        String codigo,
        String mensagem,
        BigDecimal valor,
        BigDecimal taxa,
        BigDecimal total,
        BigDecimal cashback,
        LocalDateTime processadoEm) {

    public static ResultadoPagamentoResponse de(ResultadoPagamento resultado) {
        return new ResultadoPagamentoResponse(
                resultado.pagamentoId(),
                resultado.status(),
                resultado.codigo(),
                resultado.mensagem(),
                resultado.valor(),
                resultado.taxa(),
                resultado.total(),
                resultado.cashback(),
                Datas.emSegundos(resultado.processadoEm()));
    }
}
