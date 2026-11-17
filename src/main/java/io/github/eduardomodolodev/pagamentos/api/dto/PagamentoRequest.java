package io.github.eduardomodolodev.pagamentos.api.dto;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Corpo de {@code POST /api/pagamentos}.
 *
 * <p>A Bean Validation barra só o que é <b>estrutural</b> (campos ausentes →
 * 400). Regras de negócio, como valor positivo, parcelas de 1 a 12 ou boleto
 * vencido, ficam com a cadeia de validadores do núcleo e voltam como
 * pagamento {@code RECUSADO} (201).
 *
 * @param contaOrigemId  conta que paga
 * @param tipo           {@code PIX}, {@code CARTAO_CREDITO} ou {@code BOLETO}
 * @param valor          valor do pagamento
 * @param destinatario   chave Pix, loja ou beneficiário do boleto
 * @param parcelas       só cartão (opcional, padrão 1)
 * @param dataVencimento só boleto
 */
public record PagamentoRequest(
        @NotBlank(message = "é obrigatório") String contaOrigemId,
        @NotNull(message = "é obrigatório") TipoPagamento tipo,
        @NotNull(message = "é obrigatório") BigDecimal valor,
        @NotNull(message = "é obrigatório") String destinatario,
        Integer parcelas,
        LocalDate dataVencimento) {

    /** Converte para o domínio usando o Builder. */
    public Pagamento paraPagamento(Clock clock) {
        Pagamento.Builder builder = Pagamento.builder()
                .contaOrigemId(contaOrigemId)
                .tipo(tipo)
                .valor(valor)
                .destinatario(destinatario)
                .dataVencimento(dataVencimento)
                .clock(clock);
        if (parcelas != null) {
            builder.parcelas(parcelas);
        }
        return builder.build();
    }
}
