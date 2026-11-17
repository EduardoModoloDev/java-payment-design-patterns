package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmsListenerTest {

    @ParameterizedTest(name = "{0} de {1} -> SMS? {2}")
    @CsvSource({
            "APROVADO, 200.00, false",
            "APROVADO, 999.99, false",
            "APROVADO, 1000.00, true",
            "APROVADO, 1500.00, true",
            "PENDENTE, 350.00, false",
            "PENDENTE, 1000.00, true",
            "RECUSADO, 10.00, true",
            "RECUSADO, 5000.00, true"
    })
    void deveDispararApenasParaRecusadosOuValoresAltos(StatusPagamento status, String valor, boolean esperaSms) {
        List<String> enviados = new ArrayList<>();
        SmsListener listener = new SmsListener(enviados::add);
        Pagamento pagamento = Resultados.pix(valor);
        String codigo = status == StatusPagamento.RECUSADO ? CodigoRecusa.SALDO_INSUFICIENTE : null;

        listener.aoFinalizar(pagamento, Resultados.finalizar(pagamento, status, codigo));

        assertThat(enviados).hasSize(esperaSms ? 1 : 0);
        if (esperaSms) {
            assertThat(enviados.get(0)).startsWith("[SMS]").contains("CONTA-001", valor);
        }
    }

    @ParameterizedTest
    @CsvSource({"RECUSADO, recusado (SALDO_INSUFICIENTE)", "APROVADO, aprovado"})
    void deveInformarOMotivoNoTexto(StatusPagamento status, String trechoEsperado) {
        List<String> enviados = new ArrayList<>();
        Pagamento pagamento = Resultados.pix("1200.00");
        String codigo = status == StatusPagamento.RECUSADO ? CodigoRecusa.SALDO_INSUFICIENTE : null;

        new SmsListener(enviados::add).aoFinalizar(pagamento, Resultados.finalizar(pagamento, status, codigo));

        assertThat(enviados).singleElement().asString().contains(trechoEsperado);
    }
}
