package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Soma {@code valor × percentual} à taxa antes de delegar
 * (ex.: cartão, taxa de processamento de 2,5% = {@code 0.025}).
 */
public class TaxaPercentualDecorator extends ProcessadorDecorator {

    private final BigDecimal percentual;

    public TaxaPercentualDecorator(BigDecimal percentual, ProcessadorPagamento delegado) {
        super(delegado);
        this.percentual = Objects.requireNonNull(percentual, "percentual não pode ser nulo");
        if (percentual.signum() < 0) {
            throw new IllegalArgumentException("Percentual não pode ser negativo: " + percentual);
        }
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        pagamento.adicionarTaxa(Dinheiro.de(pagamento.getValor().multiply(percentual)));
        return delegado.processar(pagamento, conta);
    }
}
