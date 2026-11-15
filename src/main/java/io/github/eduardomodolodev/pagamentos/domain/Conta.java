package io.github.eduardomodolodev.pagamentos.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Conta bancária fictícia do pagador.
 *
 * <p>Pix e boleto usam o {@code saldo}; cartão de crédito usa o
 * {@code limiteCartaoDisponivel}. Os métodos de débito são a defesa final:
 * normalmente as validações barram o pagamento antes de chegar aqui.
 */
public class Conta {

    public static final BigDecimal LIMITE_DIARIO_PADRAO = Dinheiro.de("5000.00");

    private final String id;
    private final String titular;
    private BigDecimal saldo;
    private BigDecimal limiteCartaoDisponivel;
    private final BigDecimal limiteDiario;

    public Conta(String id, String titular, BigDecimal saldo, BigDecimal limiteCartaoDisponivel) {
        this(id, titular, saldo, limiteCartaoDisponivel, LIMITE_DIARIO_PADRAO);
    }

    public Conta(String id, String titular, BigDecimal saldo, BigDecimal limiteCartaoDisponivel,
                 BigDecimal limiteDiario) {
        this.id = exigirTexto(id, "id");
        this.titular = exigirTexto(titular, "titular");
        this.saldo = exigirNaoNegativo(saldo, "saldo");
        this.limiteCartaoDisponivel = exigirNaoNegativo(limiteCartaoDisponivel, "limiteCartaoDisponivel");
        this.limiteDiario = exigirNaoNegativo(limiteDiario, "limiteDiario");
    }

    /**
     * Debita o valor do saldo da conta.
     *
     * @throws IllegalArgumentException se o valor não for positivo
     * @throws IllegalStateException    se o saldo for insuficiente
     */
    public void debitarSaldo(BigDecimal valor) {
        BigDecimal debito = exigirPositivo(valor);
        if (saldo.compareTo(debito) < 0) {
            throw new IllegalStateException(
                    "Saldo insuficiente na conta %s: saldo %s, débito %s".formatted(id, saldo, debito));
        }
        saldo = saldo.subtract(debito);
    }

    /**
     * Debita o valor do limite disponível do cartão de crédito.
     *
     * @throws IllegalArgumentException se o valor não for positivo
     * @throws IllegalStateException    se o limite for insuficiente
     */
    public void debitarLimiteCartao(BigDecimal valor) {
        BigDecimal debito = exigirPositivo(valor);
        if (limiteCartaoDisponivel.compareTo(debito) < 0) {
            throw new IllegalStateException(
                    "Limite do cartão insuficiente na conta %s: limite %s, débito %s"
                            .formatted(id, limiteCartaoDisponivel, debito));
        }
        limiteCartaoDisponivel = limiteCartaoDisponivel.subtract(debito);
    }

    public boolean temSaldoPara(BigDecimal valor) {
        return saldo.compareTo(valor) >= 0;
    }

    public boolean temLimiteCartaoPara(BigDecimal valor) {
        return limiteCartaoDisponivel.compareTo(valor) >= 0;
    }

    public String getId() {
        return id;
    }

    public String getTitular() {
        return titular;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getLimiteCartaoDisponivel() {
        return limiteCartaoDisponivel;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório da conta: " + campo);
        }
        return valor;
    }

    private static BigDecimal exigirNaoNegativo(BigDecimal valor, String campo) {
        BigDecimal normalizado = Dinheiro.de(Objects.requireNonNull(valor, campo + " não pode ser nulo"));
        if (normalizado.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return normalizado;
    }

    private static BigDecimal exigirPositivo(BigDecimal valor) {
        BigDecimal normalizado = Dinheiro.de(valor);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("Valor de débito deve ser positivo: " + valor);
        }
        return normalizado;
    }

    @Override
    public String toString() {
        return "Conta[id=%s, titular=%s, saldo=%s, limiteCartao=%s, limiteDiario=%s]"
                .formatted(id, titular, saldo, limiteCartaoDisponivel, limiteDiario);
    }
}
