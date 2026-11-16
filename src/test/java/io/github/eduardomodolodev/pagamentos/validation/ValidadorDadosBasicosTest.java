package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.MEIO_DIA;
import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.pagamento;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDadosBasicosTest {

    private static final LocalDate HOJE = MEIO_DIA.toLocalDate();

    private final ValidadorDadosBasicos validador = new ValidadorDadosBasicos(Fixtures.relogioEm(MEIO_DIA));
    private final Conta conta = Fixtures.conta("1000", "1000");

    private void assertRecusa(Pagamento pagamento, String codigo) {
        assertThatThrownBy(() -> validador.validar(pagamento, conta))
                .isInstanceOf(ValidacaoException.class)
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void deveAprovarPixValido() {
        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "10.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5.00"})
    void deveRecusarValorNaoPositivo(String valor) {
        assertRecusa(pagamento(TipoPagamento.PIX, valor).build(), CodigoRecusa.DADOS_INVALIDOS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void deveRecusarDestinatarioVazio(String destinatario) {
        assertRecusa(pagamento(TipoPagamento.PIX, "10.00").destinatario(destinatario).build(),
                CodigoRecusa.DADOS_INVALIDOS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 12})
    void deveAprovarCartaoComParcelasEntreUmEDoze(int parcelas) {
        Pagamento cartao = pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").parcelas(parcelas).build();

        assertThatCode(() -> validador.validar(cartao, conta)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 13})
    void deveRecusarCartaoComParcelasForaDaFaixa(int parcelas) {
        assertRecusa(pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").parcelas(parcelas).build(),
                CodigoRecusa.PARCELAS_INVALIDAS);
    }

    @Test
    void naoDeveOlharParcelasDeQuemNaoECartao() {
        Pagamento pix = pagamento(TipoPagamento.PIX, "10.00").parcelas(99).build();

        assertThatCode(() -> validador.validar(pix, conta)).doesNotThrowAnyException();
    }

    @Test
    void deveAprovarBoletoQueVenceHojeOuDepois() {
        Pagamento venceHoje = pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(HOJE).build();
        Pagamento venceDepois = pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(HOJE.plusDays(10)).build();

        assertThatCode(() -> validador.validar(venceHoje, conta)).doesNotThrowAnyException();
        assertThatCode(() -> validador.validar(venceDepois, conta)).doesNotThrowAnyException();
    }

    @Test
    void deveRecusarBoletoVencido() {
        assertRecusa(pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(HOJE.minusDays(1)).build(),
                CodigoRecusa.BOLETO_VENCIDO);
    }

    @Test
    void deveRecusarBoletoSemVencimento() {
        assertRecusa(pagamento(TipoPagamento.BOLETO, "350.00").build(), CodigoRecusa.DADOS_INVALIDOS);
    }
}
