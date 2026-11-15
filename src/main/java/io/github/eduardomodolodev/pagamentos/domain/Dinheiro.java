package io.github.eduardomodolodev.pagamentos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário para padronizar valores monetários.
 *
 * <p>Todo dinheiro do sistema é {@link BigDecimal} com escala 2 e
 * arredondamento {@link RoundingMode#HALF_UP} — nunca {@code double}/{@code float}.
 */
public final class Dinheiro {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA, ARREDONDAMENTO);

    private Dinheiro() {
    }

    /** Normaliza o valor para escala 2 (HALF_UP). */
    public static BigDecimal de(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor monetário não pode ser nulo");
        }
        return valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    /** Atalho para criar um valor a partir de texto, ex.: {@code Dinheiro.de("1200.00")}. */
    public static BigDecimal de(String valor) {
        return de(new BigDecimal(valor));
    }
}
