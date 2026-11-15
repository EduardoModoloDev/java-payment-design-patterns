package io.github.eduardomodolodev.pagamentos.factory;

import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.BoletoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.CartaoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.PixProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <b>Padrão Factory Method (com registro):</b> escolhe e monta o processador
 * correto para cada {@link TipoPagamento}.
 *
 * <p>Em vez de um {@code switch} que precisaria ser editado a cada novo tipo,
 * a fábrica mantém um mapa de {@link Supplier}. Um novo meio de pagamento é
 * adicionado com {@link #registrar(TipoPagamento, Supplier)}, sem alterar esta
 * classe.
 */
public class ProcessadorFactory {

    private final Map<TipoPagamento, Supplier<ProcessadorPagamento>> registro =
            new EnumMap<>(TipoPagamento.class);

    public ProcessadorFactory() {
        this(Clock.systemDefaultZone());
    }

    public ProcessadorFactory(Clock clock) {
        Objects.requireNonNull(clock, "clock não pode ser nulo");
        registrar(TipoPagamento.PIX, () -> new PixProcessador(clock));
        registrar(TipoPagamento.CARTAO_CREDITO, () -> new CartaoProcessador(clock));
        registrar(TipoPagamento.BOLETO, () -> new BoletoProcessador(clock));
    }

    /**
     * Registra (ou substitui) a montagem do processador de um tipo.
     */
    public void registrar(TipoPagamento tipo, Supplier<ProcessadorPagamento> montagem) {
        registro.put(Objects.requireNonNull(tipo, "tipo não pode ser nulo"),
                Objects.requireNonNull(montagem, "montagem não pode ser nula"));
    }

    /**
     * Cria o processador para o tipo informado.
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
