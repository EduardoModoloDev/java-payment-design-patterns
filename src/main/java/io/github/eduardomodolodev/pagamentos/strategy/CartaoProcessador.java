package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Núcleo do cartão de crédito: aprovação imediata, debitando o limite do
 * cartão (não o saldo).
 *
 * <p>Taxa de 2,5%, juros de parcelamento e cashback são responsabilidade dos
 * decorators; aqui só existe a operação em si, com a verificação final de
 * {@code limite ≥ valor + taxa}.
 */
public class CartaoProcessador implements ProcessadorPagamento {

    private final Clock clock;

    public CartaoProcessador() {
        this(Clock.systemDefaultZone());
    }

    public CartaoProcessador(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        if (!conta.temLimiteCartaoPara(pagamento.getValorTotal())) {
            pagamento.atualizarStatus(StatusPagamento.RECUSADO);
            return ResultadoPagamento.de(pagamento, CodigoRecusa.LIMITE_CARTAO_INSUFICIENTE,
                    "Limite do cartão insuficiente", LocalDateTime.now(clock));
        }

        conta.debitarLimiteCartao(pagamento.getValorTotal());
        pagamento.atualizarStatus(StatusPagamento.APROVADO);
        return ResultadoPagamento.de(pagamento, null,
                "Pagamento no cartão aprovado em %dx".formatted(pagamento.getParcelas()),
                LocalDateTime.now(clock));
    }
}
