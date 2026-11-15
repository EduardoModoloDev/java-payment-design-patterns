package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;

/**
 * <b>Padrão Strategy:</b> cada meio de pagamento tem sua própria regra de
 * processamento, encapsulada em uma implementação desta interface. Assim não há
 * um {@code if/else} gigante por tipo, e um novo meio de pagamento é só uma
 * nova classe.
 */
public interface ProcessadorPagamento {

    /**
     * Processa o pagamento contra a conta, atualizando o status do pagamento
     * e, quando for o caso, debitando a conta.
     */
    ResultadoPagamento processar(Pagamento pagamento, Conta conta);
}
