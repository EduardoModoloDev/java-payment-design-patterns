package io.github.eduardomodolodev.pagamentos.factory;

import io.github.eduardomodolodev.pagamentos.decorator.CashbackDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.JurosParcelamentoDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.LogDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.TaxaFixaDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.TaxaPercentualDecorator;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.BoletoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.CartaoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.PixProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <b>Padrão Factory Method (com registro):</b> escolhe e monta o processador
 * correto para cada {@link TipoPagamento}, já embrulhado nos decorators.
 *
 * <p>Em vez de um {@code switch} que precisaria ser editado a cada novo tipo,
 * a fábrica mantém um mapa de {@link Supplier}. Um novo meio de pagamento é
 * adicionado com {@link #registrar(TipoPagamento, Supplier)}, sem alterar esta
 * classe.
 *
 * <p>Composição (de fora para dentro):
 * <ul>
 *     <li>Pix: Log → PixProcessador</li>
 *     <li>Cartão: Log → Cashback 1% → Taxa 2,5% → Juros de parcelamento → CartaoProcessador</li>
 *     <li>Boleto: Log → Taxa fixa R$ 3,50 → BoletoProcessador</li>
 * </ul>
 */
public class ProcessadorFactory {

    public static final BigDecimal TAXA_CARTAO = new BigDecimal("0.025");
    public static final BigDecimal CASHBACK_CARTAO = new BigDecimal("0.01");
    public static final BigDecimal TAXA_BOLETO = new BigDecimal("3.50");

    private final Map<TipoPagamento, Supplier<ProcessadorPagamento>> registro =
            new EnumMap<>(TipoPagamento.class);

    public ProcessadorFactory() {
        this(Clock.systemDefaultZone());
    }

    public ProcessadorFactory(Clock clock) {
        Objects.requireNonNull(clock, "clock não pode ser nulo");

        registrar(TipoPagamento.PIX,
                () -> new LogDecorator(
                        new PixProcessador(clock)));

        registrar(TipoPagamento.CARTAO_CREDITO,
                () -> new LogDecorator(
                        new CashbackDecorator(CASHBACK_CARTAO,
                                new TaxaPercentualDecorator(TAXA_CARTAO,
                                        new JurosParcelamentoDecorator(
                                                new CartaoProcessador(clock))))));

        registrar(TipoPagamento.BOLETO,
                () -> new LogDecorator(
                        new TaxaFixaDecorator(TAXA_BOLETO,
                                new BoletoProcessador(clock))));
    }

    /**
     * Registra (ou substitui) a montagem do processador de um tipo.
     */
    public void registrar(TipoPagamento tipo, Supplier<ProcessadorPagamento> montagem) {
        registro.put(Objects.requireNonNull(tipo, "tipo não pode ser nulo"),
                Objects.requireNonNull(montagem, "montagem não pode ser nula"));
    }

    /**
     * Cria o processador (já decorado) para o tipo informado.
     *
     * @throws IllegalArgumentException se o tipo não estiver registrado
     */
    public ProcessadorPagamento criar(TipoPagamento tipo) {
        Supplier<ProcessadorPagamento> montagem = tipo == null ? null : registro.get(tipo);
        if (montagem == null) {
            throw new IllegalArgumentException("Tipo de pagamento não suportado: " + tipo);
        }
        return montagem.get();
    }

    public boolean suporta(TipoPagamento tipo) {
        return tipo != null && registro.containsKey(tipo);
    }
}
