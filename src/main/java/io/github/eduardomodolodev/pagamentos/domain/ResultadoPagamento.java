package io.github.eduardomodolodev.pagamentos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado imutável do processamento de um pagamento.
 *
 * @param pagamentoId  id do pagamento processado
 * @param status       status final ({@code APROVADO}, {@code PENDENTE} ou {@code RECUSADO})
 * @param codigo       {@code null} se aprovado/pendente; código estável do erro se recusado
 * @param mensagem     descrição legível do resultado
 * @param valor        valor original do pagamento
 * @param taxa         soma das taxas e juros aplicados
 * @param total        {@code valor + taxa}
 * @param cashback     cashback calculado (informativo)
 * @param processadoEm momento do processamento
 */
public record ResultadoPagamento(
        String pagamentoId,
        StatusPagamento status,
        String codigo,
        String mensagem,
        BigDecimal valor,
        BigDecimal taxa,
        BigDecimal total,
        BigDecimal cashback,
        LocalDateTime processadoEm) {

    /**
     * Monta o resultado a partir do estado atual do pagamento
     * (status, valor, taxa e cashback são lidos dele).
     */
    public static ResultadoPagamento de(Pagamento pagamento, String codigo, String mensagem,
                                        LocalDateTime processadoEm) {
        return new ResultadoPagamento(
                pagamento.getId(),
                pagamento.getStatus(),
                codigo,
                mensagem,
                pagamento.getValor(),
                pagamento.getTaxa(),
                pagamento.getValorTotal(),
                pagamento.getCashback(),
                processadoEm);
    }

    public boolean aprovado() {
        return status == StatusPagamento.APROVADO;
    }

    public boolean recusado() {
        return status == StatusPagamento.RECUSADO;
    }
}
