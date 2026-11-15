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
 * Núcleo do Pix: aprovação imediata, debitando o saldo da conta.
 *
 * <p>Faz a verificação final de {@code saldo ≥ valor + taxa} ("defesa em
 * profundidade"), já que as taxas só são conhecidas depois das validações.
 */
public class PixProcessador implements ProcessadorPagamento {

    private final Clock clock;

    public PixProcessador() {
        this(Clock.systemDefaultZone());
    }

    public PixProcessador(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        if (!conta.temSaldoPara(pagamento.getValorTotal())) {
            pagamento.atualizarStatus(StatusPagamento.RECUSADO);
            return ResultadoPagamento.de(pagamento, CodigoRecusa.SALDO_INSUFICIENTE,
                    "Saldo insuficiente para o Pix", LocalDateTime.now(clock));
        }

        conta.debitarSaldo(pagamento.getValorTotal());
        pagamento.atualizarStatus(StatusPagamento.APROVADO);
        return ResultadoPagamento.de(pagamento, null, "Pix aprovado", LocalDateTime.now(clock));
    }
}
