package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.pagamento;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorSaldoOuLimiteTest {

    private final ValidadorSaldoOuLimite validador = new ValidadorSaldoOuLimite();
    private final Conta conta = Fixtures.conta("500.00", "1000.00");

    private void assertRecusa(Pagamento pagamento, String codigo) {
        assertThatThrownBy(() -> validador.validar(pagamento, conta))
                .isInstanceOf(ValidacaoException.class)
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void deveAprovarPixAteOSaldo() {
        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "500.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRecusarPixMaiorQueOSaldo() {
        assertRecusa(pagamento(TipoPagamento.PIX, "500.01").build(), CodigoRecusa.SALDO_INSUFICIENTE);
    }

    @Test
    void deveUsarOSaldoParaBoleto() {
        Pagamento boleto = pagamento(TipoPagamento.BOLETO, "600.00")
                .dataVencimento(LocalDate.of(2026, 12, 1))
                .build();

        assertRecusa(boleto, CodigoRecusa.SALDO_INSUFICIENTE);
    }

    @Test
    void deveUsarOLimiteDoCartaoEnaoOSaldo() {
        Pagamento cartaoAcimaDoSaldo = pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").build();

        assertThatCode(() -> validador.validar(cartaoAcimaDoSaldo, conta)).doesNotThrowAnyException();
    }

    @Test
    void deveRecusarCartaoMaiorQueOLimite() {
        assertRecusa(pagamento(TipoPagamento.CARTAO_CREDITO, "1000.01").build(),
                CodigoRecusa.LIMITE_CARTAO_INSUFICIENTE);
    }
}
