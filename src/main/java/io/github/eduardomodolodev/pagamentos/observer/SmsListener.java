package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Simula o envio de SMS, mas só quando vale a pena incomodar o cliente:
 * pagamento <b>recusado</b> ou de valor <b>≥ R$ 1.000,00</b>.
 */
public class SmsListener implements PagamentoListener {

    public static final BigDecimal VALOR_MINIMO_PARA_SMS = Dinheiro.de("1000.00");

    private final Consumer<String> saida;

    public SmsListener() {
        this(System.out::println);
    }

    public SmsListener(Consumer<String> saida) {
        this.saida = Objects.requireNonNull(saida, "saida não pode ser nula");
    }

    @Override
    public void aoFinalizar(Pagamento pagamento, ResultadoPagamento resultado) {
        if (!deveNotificar(resultado)) {
            return;
        }
        String motivo = resultado.recusado()
                ? "recusado (" + resultado.codigo() + ")"
                : resultado.status().name().toLowerCase();
        saida.accept("[SMS] Conta %s: %s de %s %s.".formatted(
                pagamento.getContaOrigemId(), pagamento.getTipo(), resultado.valor(), motivo));
    }

    /** Recusado ou valor de pelo menos R$ 1.000,00. */
    public boolean deveNotificar(ResultadoPagamento resultado) {
        return resultado.recusado() || resultado.valor().compareTo(VALOR_MINIMO_PARA_SMS) >= 0;
    }
}
