package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

/**
 * Confere se a conta comporta o valor do pagamento.
 *
 * <ul>
 *     <li>cartão de crédito: {@code limiteCartaoDisponivel ≥ valor}
 *         ({@code LIMITE_CARTAO_INSUFICIENTE});</li>
 *     <li>demais tipos (Pix, boleto): {@code saldo ≥ valor}
 *         ({@code SALDO_INSUFICIENTE}).</li>
 * </ul>
 *
 * <p>Checa só o {@code valor}: as taxas ainda não são conhecidas aqui (os
 * decorators as somam depois). Por isso cada processador faz uma verificação
 * final de {@code valor + taxa} — defesa em profundidade.
 */
public class ValidadorSaldoOuLimite extends Validador {

    @Override
    protected void verificar(Pagamento pagamento, Conta conta) {
        if (pagamento.getTipo() == TipoPagamento.CARTAO_CREDITO) {
            if (!conta.temLimiteCartaoPara(pagamento.getValor())) {
                throw new ValidacaoException(CodigoRecusa.LIMITE_CARTAO_INSUFICIENTE,
                        "Limite do cartão insuficiente: disponível %s, valor %s"
                                .formatted(conta.getLimiteCartaoDisponivel(), pagamento.getValor()));
            }
            return;
        }

        if (!conta.temSaldoPara(pagamento.getValor())) {
            throw new ValidacaoException(CodigoRecusa.SALDO_INSUFICIENTE,
                    "Saldo insuficiente: disponível %s, valor %s"
                            .formatted(conta.getSaldo(), pagamento.getValor()));
        }
    }
}
