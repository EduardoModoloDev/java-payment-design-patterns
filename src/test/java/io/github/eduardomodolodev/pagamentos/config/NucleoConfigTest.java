package io.github.eduardomodolodev.pagamentos.config;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.observer.AuditoriaListener;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NucleoConfigTest {

    @Autowired
    private PagamentoService service;

    @Autowired
    private ContaRepository contas;

    @Autowired
    private AuditoriaListener auditoria;

    @Test
    void deveCarregarAsContasDeDemonstracao() {
        assertThat(contas.buscarPorId("CONTA-001")).isPresent();
        assertThat(contas.buscarPorId("CONTA-002")).isPresent();
        assertThat(contas.buscarPorId("CONTA-003")).isPresent();
    }

    @Test
    void deveMontarAFacadeComDecoratorsEListeners() {
        int eventosAntes = auditoria.getEventos().size();

        ResultadoPagamento resultado = service.processar(Pagamento.builder()
                .contaOrigemId("CONTA-003")
                .tipo(TipoPagamento.CARTAO_CREDITO)
                .valor(new BigDecimal("1200.00"))
                .destinatario("Loja do Zé")
                .parcelas(6)
                .build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.total()).isEqualByComparingTo("1284.00");
        assertThat(auditoria.getEventos()).hasSize(eventosAntes + 1);
    }
}
