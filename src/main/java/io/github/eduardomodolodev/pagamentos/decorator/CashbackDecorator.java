package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Depois de um pagamento aprovado, calcula o cashback sobre o valor original
 * (sem taxa/juros). É apenas informativo no resultado (ex.: cartão, 1%).
 */
public class CashbackDecorator extends ProcessadorDecorator {

    private final BigDecimal percentual;

    public CashbackDecorator(BigDecimal percentual, ProcessadorPagamento delegado) {
        super(delegado);
        this.percentual = Objects.requireNonNull(percentual, "percentual não pode ser nulo");
        if (percentual.signum() < 0) {
            throw new IllegalArgumentException("Percentual não pode ser negativo: " + percentual);
        }
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        ResultadoPagamento resultado = delegado.processar(pagamento, conta);
        if (!resultado.aprovado()) {
            return resultado;
        }

        pagamento.adicionarCashback(Dinheiro.de(pagamento.getValor().multiply(percentual)));
        return ResultadoPagamento.de(pagamento, resultado.codigo(), resultado.mensagem(), resultado.processadoEm());
    }
}
