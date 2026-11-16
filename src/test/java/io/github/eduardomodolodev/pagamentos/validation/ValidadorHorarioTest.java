package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.pagamento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorHorarioTest {

    private final Conta conta = Fixtures.conta("10000", "10000");

    private static ValidadorHorario validadorAs(String horario) {
        return new ValidadorHorario(Fixtures.relogioEm(
                LocalDateTime.of(Fixtures.MEIO_DIA.toLocalDate(), LocalTime.parse(horario))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"20:00", "22:00", "23:59", "00:00", "05:59"})
    void deveRecusarPixAcimaDeMilNoPeriodoNoturno(String horario) {
        assertThatThrownBy(() -> validadorAs(horario).validar(pagamento(TipoPagamento.PIX, "1500.00").build(), conta))
                .isInstanceOf(ValidacaoException.class)
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(CodigoRecusa.LIMITE_NOTURNO_EXCEDIDO);
    }

    @Test
    void deveAprovarPixDeExatamenteMilANoite() {
        assertThatCode(() -> validadorAs("22:00").validar(pagamento(TipoPagamento.PIX, "1000.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"06:00", "12:00", "19:59"})
    void deveAprovarPixAltoDuranteODia(String horario) {
        assertThatCode(() -> validadorAs(horario).validar(pagamento(TipoPagamento.PIX, "1500.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void naoSeAplicaAOutrosTipos() {
        assertThatCode(() -> validadorAs("22:00")
                .validar(pagamento(TipoPagamento.CARTAO_CREDITO, "1500.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void deveIdentificarAFaixaNoturna() {
        assertThat(ValidadorHorario.ehNoturno(LocalTime.of(20, 0))).isTrue();
        assertThat(ValidadorHorario.ehNoturno(LocalTime.of(5, 59))).isTrue();
        assertThat(ValidadorHorario.ehNoturno(LocalTime.of(6, 0))).isFalse();
        assertThat(ValidadorHorario.ehNoturno(LocalTime.of(19, 59))).isFalse();
    }
}
