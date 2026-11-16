package io.github.eduardomodolodev.pagamentos.factory;

import io.github.eduardomodolodev.pagamentos.decorator.CashbackDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.JurosParcelamentoDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.LogDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.ProcessadorDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.TaxaFixaDecorator;
import io.github.eduardomodolodev.pagamentos.decorator.TaxaPercentualDecorator;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.BoletoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.CartaoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.PixProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessadorFactoryTest {

    private final ProcessadorFactory factory = new ProcessadorFactory();

    /** Desembrulha os decorators e devolve as classes, de fora para dentro. */
    private static List<Class<?>> camadas(ProcessadorPagamento processador) {
        List<Class<?>> camadas = new ArrayList<>();
        ProcessadorPagamento atual = processador;
        while (atual instanceof ProcessadorDecorator decorator) {
            camadas.add(decorator.getClass());
            atual = decorator.getDelegado();
        }
        camadas.add(atual.getClass());
        return camadas;
    }

    private static Pagamento.Builder pagamento(TipoPagamento tipo, String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .destinatario("Favorecido");
    }

    @Test
    void deveComporPixComLog() {
        assertThat(camadas(factory.criar(TipoPagamento.PIX)))
                .containsExactly(LogDecorator.class, PixProcessador.class);
    }

    @Test
    void deveComporCartaoComCashbackTaxaEJuros() {
        assertThat(camadas(factory.criar(TipoPagamento.CARTAO_CREDITO))).containsExactly(
                LogDecorator.class,
                CashbackDecorator.class,
                TaxaPercentualDecorator.class,
                JurosParcelamentoDecorator.class,
                CartaoProcessador.class);
    }

    @Test
    void deveComporBoletoComTaxaFixa() {
        assertThat(camadas(factory.criar(TipoPagamento.BOLETO)))
                .containsExactly(LogDecorator.class, TaxaFixaDecorator.class, BoletoProcessador.class);
    }

    @Test
    void cartaoMontadoPelaFactoryDeveCalcularOExemploDaEspecificacao() {
        Conta conta = new Conta("CONTA-001", "Maria", BigDecimal.ZERO, new BigDecimal("5000"));

        ResultadoPagamento resultado = factory.criar(TipoPagamento.CARTAO_CREDITO)
                .processar(pagamento(TipoPagamento.CARTAO_CREDITO, "1200.00").parcelas(6).build(), conta);

        assertThat(resultado.taxa()).isEqualByComparingTo("84.00");
        assertThat(resultado.total()).isEqualByComparingTo("1284.00");
        assertThat(resultado.cashback()).isEqualByComparingTo("12.00");
    }

    @Test
    void boletoMontadoPelaFactoryDeveTerTaxaFixa() {
        Conta conta = new Conta("CONTA-001", "Maria", new BigDecimal("1000"), BigDecimal.ZERO);

        ResultadoPagamento resultado = factory.criar(TipoPagamento.BOLETO).processar(
                pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(LocalDate.of(2026, 12, 1)).build(), conta);

        assertThat(resultado.taxa()).isEqualByComparingTo("3.50");
        assertThat(resultado.total()).isEqualByComparingTo("353.50");
    }

    @Test
    void pixMontadoPelaFactoryNaoDeveTerTaxa() {
        Conta conta = new Conta("CONTA-001", "Maria", new BigDecimal("1000"), BigDecimal.ZERO);

        ResultadoPagamento resultado = factory.criar(TipoPagamento.PIX)
                .processar(pagamento(TipoPagamento.PIX, "200.00").build(), conta);

        assertThat(resultado.taxa()).isEqualByComparingTo("0");
        assertThat(conta.getSaldo()).isEqualByComparingTo("800.00");
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
