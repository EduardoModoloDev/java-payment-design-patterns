package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailListenerTest {

    private final List<String> enviados = new ArrayList<>();
    private final EmailListener listener = new EmailListener(enviados::add);

    @Test
    void deveEnviarEmailParaPagamentoAprovado() {
        Pagamento pagamento = Resultados.pix("200.00");

        listener.aoFinalizar(pagamento, Resultados.finalizar(pagamento, StatusPagamento.APROVADO, null));

        assertThat(enviados).singleElement().asString()
                .startsWith("[EMAIL]")
                .contains("CONTA-001", "PIX", "200.00", "APROVADO");
    }

    @Test
    void deveEnviarEmailTambemParaPagamentoRecusadoComOCodigo() {
        Pagamento pagamento = Resultados.pix("50.00");

        listener.aoFinalizar(pagamento,
                Resultados.finalizar(pagamento, StatusPagamento.RECUSADO, CodigoRecusa.SALDO_INSUFICIENTE));

        assertThat(enviados).singleElement().asString()
                .contains("RECUSADO", "SALDO_INSUFICIENTE");
    }
}
