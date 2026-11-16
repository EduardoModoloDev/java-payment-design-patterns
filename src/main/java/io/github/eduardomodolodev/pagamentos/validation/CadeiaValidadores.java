package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * Monta a cadeia padrão de validações, das mais baratas para as mais caras:
 *
 * <pre>
 * ValidadorDadosBasicos → ValidadorHorario → ValidadorSaldoOuLimite
 *     → ValidadorLimiteDiario → ValidadorAntifraude
 * </pre>
 */
public final class CadeiaValidadores {

    private CadeiaValidadores() {
    }

    /** Devolve o primeiro elo da cadeia padrão. */
    public static Validador padrao(PagamentoRepository pagamentoRepository, Clock clock) {
        Objects.requireNonNull(pagamentoRepository, "pagamentoRepository");
        Objects.requireNonNull(clock, "clock");

        Validador inicio = new ValidadorDadosBasicos(clock);
        inicio.proximo(new ValidadorHorario(clock))
                .proximo(new ValidadorSaldoOuLimite())
                .proximo(new ValidadorLimiteDiario(pagamentoRepository))
                .proximo(new ValidadorAntifraude(pagamentoRepository));
        return inicio;
    }
}
