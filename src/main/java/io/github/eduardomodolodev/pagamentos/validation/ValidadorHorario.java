package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Restrição noturna do Pix: entre 20h e 6h, cada transação pode ter no máximo
 * R$ 1.000,00 ({@code LIMITE_NOTURNO_EXCEDIDO}).
 *
 * <p>O horário vem de um {@link Clock} injetado, para que a regra seja testável
 * sem depender do relógio real.
 */
public class ValidadorHorario extends Validador {

    public static final LocalTime INICIO_NOTURNO = LocalTime.of(20, 0);
    public static final LocalTime FIM_NOTURNO = LocalTime.of(6, 0);
    public static final BigDecimal LIMITE_NOTURNO_PIX = Dinheiro.de("1000.00");

    private final Clock clock;

    public ValidadorHorario(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    @Override
    protected void verificar(Pagamento pagamento, Conta conta) {
        if (pagamento.getTipo() != TipoPagamento.PIX) {
            return;
        }
        LocalTime agora = LocalTime.now(clock);
        if (ehNoturno(agora) && pagamento.getValor().compareTo(LIMITE_NOTURNO_PIX) > 0) {
            throw new ValidacaoException(CodigoRecusa.LIMITE_NOTURNO_EXCEDIDO,
                    "Pix noturno (20h às 6h) limitado a %s por transação; valor: %s"
                            .formatted(LIMITE_NOTURNO_PIX, pagamento.getValor()));
        }
    }

    /** Das 20:00 (inclusive) às 06:00 (exclusive). */
    static boolean ehNoturno(LocalTime horario) {
        return !horario.isBefore(INICIO_NOTURNO) || horario.isBefore(FIM_NOTURNO);
    }
}
