package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.math.BigDecimal;

/**
 * Soma uma taxa fixa ao pagamento antes de delegar (ex.: boleto, R$ 3,50).
 */
public class TaxaFixaDecorator extends ProcessadorDecorator {

    private final BigDecimal taxa;

    public TaxaFixaDecorator(BigDecimal taxa, ProcessadorPagamento delegado) {
        super(delegado);
        this.taxa = Dinheiro.de(taxa);
        if (this.taxa.signum() < 0) {
            throw new IllegalArgumentException("Taxa fixa não pode ser negativa: " + taxa);
        }
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        pagamento.adicionarTaxa(taxa);
        return delegado.processar(pagamento, conta);
    }
}
