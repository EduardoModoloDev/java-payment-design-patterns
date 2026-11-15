package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Núcleo do boleto: apenas gera o boleto, com status {@code PENDENTE}.
 *
 * <p>Nada é debitado na criação; o débito acontece quando o boleto for
 * confirmado ({@code confirmarBoleto}).
 */
public class BoletoProcessador implements ProcessadorPagamento {

    private final Clock clock;

    public BoletoProcessador() {
        this(Clock.systemDefaultZone());
    }

    public BoletoProcessador(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        pagamento.atualizarStatus(StatusPagamento.PENDENTE);
        String mensagem = pagamento.getDataVencimento() == null
                ? "Boleto gerado, aguardando pagamento"
                : "Boleto gerado com vencimento em %s, aguardando pagamento"
                        .formatted(pagamento.getDataVencimento());
        return ResultadoPagamento.de(pagamento, null, mensagem, LocalDateTime.now(clock));
    }
}
