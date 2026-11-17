package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;

/**
 * <b>Padrão Observer:</b> interessados em saber quando um pagamento termina
 * (e-mail, SMS, auditoria...) se registram no {@code PagamentoService} e são
 * avisados ao final de cada processamento, sem que o pagamento conheça nenhum
 * deles.
 *
 * <p>Interface própria, já que {@code java.util.Observer} está obsoleta.
 */
@FunctionalInterface
public interface PagamentoListener {

    /**
     * Chamado ao final de cada pagamento (aprovado, pendente ou recusado) e de
     * cada confirmação de boleto.
     */
    void aoFinalizar(Pagamento pagamento, ResultadoPagamento resultado);
}
