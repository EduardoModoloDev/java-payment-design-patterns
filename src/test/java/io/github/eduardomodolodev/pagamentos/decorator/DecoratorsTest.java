package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.BoletoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.CartaoProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.PixProcessador;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecoratorsTest {

    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.025");
    private static final BigDecimal CASHBACK_CARTAO = new BigDecimal("0.01");

    private static Conta conta(String saldo, String limiteCartao) {
        return new Conta("CONTA-001", "Maria", new BigDecimal(saldo), new BigDecimal(limiteCartao));
    }

    private static Pagamento cartao(String valor, int parcelas) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.CARTAO_CREDITO)
                .valor(new BigDecimal(valor))
                .destinatario("Loja do Zé")
                .parcelas(parcelas)
                .build();
    }

    private static ProcessadorPagamento cartaoCompleto() {
        return new CashbackDecorator(CASHBACK_CARTAO,
                new TaxaPercentualDecorator(TAXA_CARTAO,
                        new JurosParcelamentoDecorator(
                                new CartaoProcessador())));
    }

    @Test
    void deveCalcularOExemploDaEspecificacaoDoCartaoEm6x() {
        Conta conta = conta("0", "5000.00");
        Pagamento pagamento = cartao("1200.00", 6);

        ResultadoPagamento resultado = cartaoCompleto().processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.taxa()).isEqualByComparingTo(new BigDecimal("84.00"));
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("1284.00"));
        assertThat(resultado.cashback()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(resultado.total().divide(BigDecimal.valueOf(6))).isEqualByComparingTo("214.00");
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("3716.00");
    }

    @ParameterizedTest(name = "{0}x de 1000,00 -> juros {1}")
    @CsvSource({
            "1, 0.00",
            "3, 0.00",
            "4, 15.00",
            "6, 45.00",
            "12, 135.00"
    })
    void deveAplicarJurosApenasAcimaDaTerceiraParcela(int parcelas, String jurosEsperados) {
        Pagamento pagamento = cartao("1000.00", parcelas);

        new JurosParcelamentoDecorator(new CartaoProcessador()).processar(pagamento, conta("0", "5000"));

        assertThat(pagamento.getTaxa()).isEqualByComparingTo(jurosEsperados);
    }

    @Test
    void deveArredondarATaxaPercentualParaDuasCasasComHalfUp() {
        Pagamento pagamento = cartao("10.10", 1);

        new TaxaPercentualDecorator(TAXA_CARTAO, new CartaoProcessador()).processar(pagamento, conta("0", "100"));

        // 10,10 × 2,5% = 0,2525 → 0,25
        assertThat(pagamento.getTaxa()).isEqualTo(new BigDecimal("0.25"));
    }

    @Test
    void deveSomarTaxaFixaDoBoleto() {
        Pagamento boleto = Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.BOLETO)
                .valor(new BigDecimal("350.00"))
                .destinatario("Companhia de Energia")
                .dataVencimento(LocalDate.of(2026, 12, 1))
                .build();

        ResultadoPagamento resultado = new TaxaFixaDecorator(new BigDecimal("3.50"), new BoletoProcessador())
                .processar(boleto, conta("1000", "0"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(resultado.taxa()).isEqualByComparingTo("3.50");
        assertThat(resultado.total()).isEqualByComparingTo("353.50");
    }

    @Test
    void taxaDeveSerAplicadaAntesDoNucleoParaODebitoIncluirATaxa() {
        // Limite cobre o valor (1.200), mas não valor + taxas (1.284):
        // o núcleo recusa na verificação final (defesa em profundidade).
        Conta conta = conta("0", "1250.00");

        ResultadoPagamento resultado = cartaoCompleto().processar(cartao("1200.00", 6), conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.LIMITE_CARTAO_INSUFICIENTE);
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("1250.00");
    }

    @Test
    void cashbackNaoDeveSerConcedidoQuandoOPagamentoForRecusado() {
        ResultadoPagamento resultado = cartaoCompleto().processar(cartao("1200.00", 6), conta("0", "100"));

        assertThat(resultado.cashback()).isEqualByComparingTo("0");
    }

    @Test
    void cashbackDeveIncidirSobreOValorSemTaxas() {
        Pagamento pagamento = cartao("1000.00", 12);

        ResultadoPagamento resultado = cartaoCompleto().processar(pagamento, conta("0", "5000"));

        assertThat(resultado.taxa()).isEqualByComparingTo("160.00");
        assertThat(resultado.cashback()).isEqualByComparingTo("10.00");
    }

    @Test
    void logDeveRegistrarEntradaESaidaSemAlterarOResultado() {
        List<String> linhas = new ArrayList<>();
        Pagamento pix = Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal("200.00"))
                .destinatario("chave@pix.com")
                .build();

        ResultadoPagamento resultado = new LogDecorator(linhas::add, new PixProcessador())
                .processar(pix, conta("1000", "0"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0)).startsWith("[LOG] >>").contains("PIX", "200.00", pix.getId());
        assertThat(linhas.get(1)).startsWith("[LOG] <<").contains("APROVADO", "total 200.00");
    }

    @Test
    void naoDeveAceitarDelegadoNuloNemPercentualNegativo() {
        assertThatThrownBy(() -> new LogDecorator(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaxaPercentualDecorator(new BigDecimal("-0.01"), new PixProcessador()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaxaFixaDecorator(new BigDecimal("-1"), new PixProcessador()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
