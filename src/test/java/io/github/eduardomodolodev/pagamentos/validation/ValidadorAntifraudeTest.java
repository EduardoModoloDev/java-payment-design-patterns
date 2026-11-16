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

class ValidadorAntifraudeTest {

    private final PagamentoRepository repositorio = new PagamentoRepositoryEmMemoria();
    private final ValidadorAntifraude validador = new ValidadorAntifraude(repositorio);
    private final Conta conta = Fixtures.conta("100000", "100000");

    private void registrar(String valor, StatusPagamento status, LocalDateTime criadoEm) {
        Pagamento pagamento = pagamento(TipoPagamento.PIX, valor, criadoEm).build();
        pagamento.atualizarStatus(status);
        repositorio.salvar(pagamento);
    }

    private void assertRecusa(Pagamento pagamento, String codigo) {
        assertThatThrownBy(() -> validador.validar(pagamento, conta))
                .isInstanceOf(ValidacaoException.class)
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void deveAprovarAteQuatroPagamentosNaJanelaDeDoisMinutos() {
        for (int i = 1; i <= 4; i++) {
            registrar("10.00", StatusPagamento.RECUSADO, MEIO_DIA.minusSeconds(10L * i));
        }

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "10.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRecusarOSextoPagamentoEmPoucosSegundos() {
        for (int i = 1; i <= 5; i++) {
            registrar("10.00", StatusPagamento.RECUSADO, MEIO_DIA.minusSeconds(10L * i));
        }

        assertRecusa(pagamento(TipoPagamento.PIX, "10.00").build(), CodigoRecusa.FRAUDE_VELOCIDADE);
    }

    @Test
    void naoDeveContarPagamentosForaDaJanela() {
        for (int i = 1; i <= 5; i++) {
            registrar("10.00", StatusPagamento.RECUSADO, MEIO_DIA.minusMinutes(2).minusSeconds(i));
        }

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "10.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void naoDeveContarOProprioPagamentoJaSalvo() {
        for (int i = 1; i <= 4; i++) {
            registrar("10.00", StatusPagamento.RECUSADO, MEIO_DIA.minusSeconds(10L * i));
        }
        Pagamento atual = pagamento(TipoPagamento.PIX, "10.00").build();
        repositorio.salvar(atual);

        assertThatCode(() -> validador.validar(atual, conta)).doesNotThrowAnyException();
    }

    @Test
    void deveRecusarValorAcimaDeCincoVezesAMediaDosAprovados() {
        registrar("100.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(3));
        registrar("200.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(2));
        registrar("300.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(1));

        assertRecusa(pagamento(TipoPagamento.PIX, "1000.01").build(), CodigoRecusa.FRAUDE_VALOR_ATIPICO);
    }

    @Test
    void deveAprovarValorDeExatamenteCincoVezesAMedia() {
        registrar("100.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(3));
        registrar("200.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(2));
        registrar("300.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(1));

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "1000.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void naoAplicaValorAtipicoComMenosDeTresAprovados() {
        registrar("100.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(2));
        registrar("100.00", StatusPagamento.APROVADO, MEIO_DIA.minusHours(1));
        registrar("100.00", StatusPagamento.RECUSADO, MEIO_DIA.minusHours(1));

        assertThatCode(() -> validador.validar(pagamento(TipoPagamento.PIX, "9000.00").build(), conta))
                .doesNotThrowAnyException();
    }
}
