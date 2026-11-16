package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.util.Objects;

/**
 * <b>Padrão Decorator:</b> base dos processadores que "embrulham" outro
 * processador para acrescentar comportamento (taxas, juros, cashback, log) sem
 * alterar o núcleo de cada meio de pagamento.
 *
 * <p>Como o decorator também é um {@link ProcessadorPagamento}, eles podem ser
 * empilhados livremente. A ordem importa: decorators de taxa agem
 * <b>antes</b> de delegar (para o núcleo debitar {@code valor + taxa});
 * cashback e log olham o resultado <b>depois</b>.
 */
public abstract class ProcessadorDecorator implements ProcessadorPagamento {

    protected final ProcessadorPagamento delegado;

    protected ProcessadorDecorator(ProcessadorPagamento delegado) {
        this.delegado = Objects.requireNonNull(delegado, "delegado não pode ser nulo");
    }

    public ProcessadorPagamento getDelegado() {
        return delegado;
    }
}
