package io.github.eduardomodolodev.pagamentos.api.dto;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Estado atual de um pagamento, usado nas consultas. */
public record PagamentoResponse(
        String id,
        String contaOrigemId,
        TipoPagamento tipo,
        BigDecimal valor,
        String destinatario,
        int parcelas,
        LocalDate dataVencimento,
        LocalDateTime criadoEm,
        BigDecimal taxa,
        BigDecimal total,
        BigDecimal cashback,
        StatusPagamento status) {

    public static PagamentoResponse de(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getContaOrigemId(),
                pagamento.getTipo(),
                pagamento.getValor(),
                pagamento.getDestinatario(),
                pagamento.getParcelas(),
                pagamento.getDataVencimento(),
                pagamento.getCriadoEm(),
                pagamento.getTaxa(),
                pagamento.getValorTotal(),
                pagamento.getCashback(),
                pagamento.getStatus());
    }
}
