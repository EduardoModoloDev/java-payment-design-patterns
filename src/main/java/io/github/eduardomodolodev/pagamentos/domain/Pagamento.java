package io.github.eduardomodolodev.pagamentos.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pagamento a ser processado pelo simulador.
 *
 * <p><b>Padrão Builder:</b> o pagamento tem campos obrigatórios (conta, tipo,
 * valor, destinatário) e opcionais que só fazem sentido para alguns tipos
 * ({@code parcelas} para cartão, {@code dataVencimento} para boleto). O
 * {@link Builder} evita um construtor gigante e garante, na construção, apenas o
 * que é estrutural; as regras de negócio ficam nos validadores.
 */
public class Pagamento {

    public static final int PARCELAS_PADRAO = 1;

    private final String id;
    private final String contaOrigemId;
    private final TipoPagamento tipo;
    private final BigDecimal valor;
    private final String destinatario;
    private final int parcelas;
    private final LocalDate dataVencimento;
    private final LocalDateTime criadoEm;

    private BigDecimal taxa = Dinheiro.ZERO;
    private BigDecimal cashback = Dinheiro.ZERO;
    private StatusPagamento status = StatusPagamento.CRIADO;

    private Pagamento(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.contaOrigemId = builder.contaOrigemId;
        this.tipo = builder.tipo;
        this.valor = Dinheiro.de(builder.valor);
        this.destinatario = builder.destinatario;
        this.parcelas = builder.parcelas;
        this.dataVencimento = builder.dataVencimento;
        this.criadoEm = LocalDateTime.now(builder.clock);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Acrescenta uma taxa (decorators somam taxas e juros por aqui). */
    public void adicionarTaxa(BigDecimal valorTaxa) {
        this.taxa = Dinheiro.de(this.taxa.add(exigirNaoNegativo(valorTaxa, "taxa")));
    }

    /** Acrescenta cashback, apenas informativo no resultado. */
    public void adicionarCashback(BigDecimal valorCashback) {
        this.cashback = Dinheiro.de(this.cashback.add(exigirNaoNegativo(valorCashback, "cashback")));
    }

    /** Valor efetivamente cobrado: {@code valor + taxa}. */
    public BigDecimal getValorTotal() {
        return Dinheiro.de(valor.add(taxa));
    }

    public void atualizarStatus(StatusPagamento novoStatus) {
        this.status = Objects.requireNonNull(novoStatus, "status não pode ser nulo");
    }

    public String getId() {
        return id;
    }

    public String getContaOrigemId() {
        return contaOrigemId;
    }

    public TipoPagamento getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public int getParcelas() {
        return parcelas;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public BigDecimal getCashback() {
        return cashback;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    private static BigDecimal exigirNaoNegativo(BigDecimal valor, String campo) {
        BigDecimal normalizado = Dinheiro.de(valor);
        if (normalizado.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo(a): " + valor);
        }
        return normalizado;
    }

    @Override
    public String toString() {
        return "Pagamento[id=%s, conta=%s, tipo=%s, valor=%s, taxa=%s, status=%s]"
                .formatted(id, contaOrigemId, tipo, valor, taxa, status);
    }

    /** Builder fluente de {@link Pagamento}. */
    public static final class Builder {

        private String contaOrigemId;
        private TipoPagamento tipo;
        private BigDecimal valor;
        private String destinatario;
        private int parcelas = PARCELAS_PADRAO;
        private LocalDate dataVencimento;
        private Clock clock = Clock.systemDefaultZone();

        private Builder() {
        }

        public Builder contaOrigemId(String contaOrigemId) {
            this.contaOrigemId = contaOrigemId;
            return this;
        }

        public Builder tipo(TipoPagamento tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder valor(BigDecimal valor) {
            this.valor = valor;
            return this;
        }

        public Builder destinatario(String destinatario) {
            this.destinatario = destinatario;
            return this;
        }

        /** Número de parcelas (cartão). Padrão: 1. A faixa válida é checada pelos validadores. */
        public Builder parcelas(int parcelas) {
            this.parcelas = parcelas;
            return this;
        }

        /** Data de vencimento (boleto). */
        public Builder dataVencimento(LocalDate dataVencimento) {
            this.dataVencimento = dataVencimento;
            return this;
        }

        /** Relógio usado para gerar {@code criadoEm}; injetável para testes. */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
            return this;
        }

        /**
         * Cria o pagamento.
         *
         * @throws IllegalStateException se algum campo obrigatório estiver ausente
         */
        public Pagamento build() {
            exigir(contaOrigemId != null && !contaOrigemId.isBlank(), "contaOrigemId");
            exigir(tipo != null, "tipo");
            exigir(valor != null, "valor");
            exigir(destinatario != null, "destinatario");
            return new Pagamento(this);
        }

        private static void exigir(boolean condicao, String campo) {
            if (!condicao) {
                throw new IllegalStateException("Campo obrigatório do pagamento não informado: " + campo);
            }
        }
    }
}
