package io.github.eduardomodolodev.pagamentos.api.dto;

import io.github.eduardomodolodev.pagamentos.domain.Conta;

import java.math.BigDecimal;

/** Dados, saldo e limites de uma conta. */
public record ContaResponse(
        String id,
        String titular,
        BigDecimal saldo,
        BigDecimal limiteCartaoDisponivel,
        BigDecimal limiteDiario) {

    public static ContaResponse de(Conta conta) {
        return new ContaResponse(
                conta.getId(),
                conta.getTitular(),
                conta.getSaldo(),
                conta.getLimiteCartaoDisponivel(),
                conta.getLimiteDiario());
    }
}
