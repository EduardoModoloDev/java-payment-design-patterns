package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;

/**
 * <b>Padrão Chain of Responsibility:</b> cada validador verifica uma única
 * regra e repassa para o próximo da cadeia.
 *
 * <p>As validações ficam independentes, ordenáveis e fáceis de estender: uma
 * regra nova é uma subclasse nova encaixada na cadeia. A primeira regra que
 * reprovar lança {@link ValidacaoException} e interrompe a cadeia.
 */
public abstract class Validador {

    private Validador proximo;

    /**
     * Define o próximo elo e o devolve, permitindo encadear de forma fluente:
     * {@code a.proximo(b).proximo(c)}.
     */
    public Validador proximo(Validador proximo) {
        this.proximo = proximo;
        return proximo;
    }

    /** Próximo elo da cadeia, ou {@code null} se este for o último. */
    public Validador getProximo() {
        return proximo;
    }

    /**
     * Executa esta verificação e, se aprovada, as seguintes.
     *
     * @throws ValidacaoException na primeira regra reprovada
     */
    public final void validar(Pagamento pagamento, Conta conta) {
        verificar(pagamento, conta);
        if (proximo != null) {
            proximo.validar(pagamento, conta);
        }
    }

    /**
     * Regra deste elo. Deve lançar {@link ValidacaoException} se reprovar.
     */
    protected abstract void verificar(Pagamento pagamento, Conta conta);
}
