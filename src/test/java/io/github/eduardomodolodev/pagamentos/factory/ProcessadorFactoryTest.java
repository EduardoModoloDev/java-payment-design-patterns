package io.github.eduardomodolodev.pagamentos.factory;

import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.BoletoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.CartaoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.PixProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessadorFactoryTest {

    private final ProcessadorFactory factory = new ProcessadorFactory();

    @Test
    void deveCriarOProcessadorCertoParaCadaTipo() {
        assertThat(factory.criar(TipoPagamento.PIX)).isInstanceOf(PixProcessador.class);
        assertThat(factory.criar(TipoPagamento.CARTAO_CREDITO)).isInstanceOf(CartaoProcessador.class);
        assertThat(factory.criar(TipoPagamento.BOLETO)).isInstanceOf(BoletoProcessador.class);
    }

    @Test
    void deveSuportarTodosOsTiposConhecidos() {
        for (TipoPagamento tipo : TipoPagamento.values()) {
            assertThat(factory.suporta(tipo)).as("suporta %s", tipo).isTrue();
        }
    }

    @Test
    void deveCriarUmaNovaInstanciaACadaChamada() {
        assertThat(factory.criar(TipoPagamento.PIX)).isNotSameAs(factory.criar(TipoPagamento.PIX));
    }

    @Test
    void deveLancarErroParaTipoNulo() {
        assertThatThrownBy(() -> factory.criar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não suportado");
        assertThat(factory.suporta(null)).isFalse();
    }

    @Test
    void devePermitirSubstituirAMontagemDeUmTipoViaRegistro() {
        ProcessadorPagamento personalizado = (pagamento, conta) ->
                ResultadoPagamento.de(pagamento, null, "processador personalizado", null);

        factory.registrar(TipoPagamento.PIX, () -> personalizado);

        assertThat(factory.criar(TipoPagamento.PIX)).isSameAs(personalizado);
    }

    @Test
    void naoDeveAceitarRegistroNulo() {
        assertThatThrownBy(() -> factory.registrar(TipoPagamento.PIX, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> factory.registrar(null, PixProcessador::new))
                .isInstanceOf(NullPointerException.class);
    }
}
