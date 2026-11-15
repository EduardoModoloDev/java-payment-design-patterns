package io.github.eduardomodolodev.pagamentos.strategy;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartaoProcessadorTest {

    private final CartaoProcessador processador = new CartaoProcessador();

    private static Conta conta(String limiteCartao) {
        return new Conta("CONTA-001", "Maria", new BigDecimal("500.00"), new BigDecimal(limiteCartao));
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

    @Test
    void deveAprovarEDebitarOLimiteSemMexerNoSaldo() {
        Conta conta = conta("3000.00");
        Pagamento pagamento = cartao("1200.00", 6);

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.codigo()).isNull();
        assertThat(resultado.mensagem()).contains("6x");
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("1800.00");
        assertThat(conta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    void deveDebitarValorMaisTaxaDoLimite() {
        Conta conta = conta("3000.00");
        Pagamento pagamento = cartao("1200.00", 6);
        pagamento.adicionarTaxa(new BigDecimal("84.00"));

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.total()).isEqualByComparingTo("1284.00");
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("1716.00");
    }

    @Test
    void deveRecusarNaVerificacaoFinalQuandoLimiteForInsuficiente() {
        Conta conta = conta("1000.00");
        Pagamento pagamento = cartao("1200.00", 1);

        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.LIMITE_CARTAO_INSUFICIENTE);
        assertThat(conta.getLimiteCartaoDisponivel()).isEqualByComparingTo("1000.00");
    }
}
