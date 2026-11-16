package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.MEIO_DIA;
import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.pagamento;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorLimiteDiarioTest {

    private final PagamentoRepository repositorio = new PagamentoRepositoryEmMemoria();
    private final ValidadorLimiteDiario validador = new ValidadorLimiteDiario(repositorio);
    private final Conta conta = Fixtures.conta("100000", "100000");

    private void registrar(TipoPagamento tipo, String valor, StatusPagamento status, LocalDateTime criadoEm) {
        Pagamento pagamento = pagamento(tipo, valor, criadoEm).build();
        pagamento.atualizarStatus(status);
        repositorio.salvar(pagamento);
    }

    @Test
    void deveAprovarQuandoASomaDoDiaFicaDentroDoLimite() {
        registrar(TipoPagamento.PIX, "4000.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(2));

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "1000.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRecusarQuandoASomaDoDiaPassaDoLimite() {
        registrar(TipoPagamento.PIX, "2000.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(3));
        registrar(TipoPagamento.PIX, "2000.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(2));

        assertThatThrownBy(() -> validador.validar(pagamento(TipoPagamento.PIX, "1500.00").build(), conta))
                .isInstanceOf(ValidacaoException.class)
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(CodigoRecusa.LIMITE_DIARIO_EXCEDIDO);
    }

    @Test
    void deveIgnorarPagamentosNaoAprovadosCartoesEDiasAnteriores() {
        registrar(TipoPagamento.PIX, "4000.00", StatusPagamento.RECUSADO, MEIO_DIA.minusHours(1));
        registrar(TipoPagamento.BOLETO, "4000.00", StatusPagamento.PENDENTE, MEIO_DIA.minusHours(1));
        registrar(TipoPagamento.CARTAO_CREDITO, "4000.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(1));
        registrar(TipoPagamento.PIX, "4000.00", StatusPagamento.APROVADO, MEIO_DIA.minusDays(1));

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "5000.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void naoSeAplicaACartao() {
        registrar(TipoPagamento.PIX, "5000.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(1));

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.CARTAO_CREDITO, "1000.00").build(), conta))
                .doesNotThrowAnyException();
    }
}
