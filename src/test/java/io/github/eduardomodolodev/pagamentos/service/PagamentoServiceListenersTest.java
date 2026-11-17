package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.observer.AuditoriaListener;
import io.github.eduardomodolodev.pagamentos.observer.PagamentoListener;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.validation.CadeiaValidadores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoServiceListenersTest {

    private static final Clock RELOGIO = Clock.fixed(
            LocalDateTime.of(2026, 11, 17, 14, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private PagamentoService service;

    @BeforeEach
    void setUp() {
        ContaRepositoryEmMemoria contas = new ContaRepositoryEmMemoria();
        contas.salvar(new Conta("CONTA-001", "Maria", new BigDecimal("1000.00"), new BigDecimal("1000.00")));
        PagamentoRepository pagamentos = new PagamentoRepositoryEmMemoria();
        service = new PagamentoService(contas, pagamentos, new ProcessadorFactory(RELOGIO),
                CadeiaValidadores.padrao(pagamentos, RELOGIO), RELOGIO);
    }

    private static Pagamento pix(String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal(valor))
                .destinatario("chave@pix.com")
                .clock(RELOGIO)
                .build();
    }

    @Test
    void deveNotificarTodosOsListenersComOMesmoResultadoDevolvido() {
        AuditoriaListener primeiro = new AuditoriaListener();
        AuditoriaListener segundo = new AuditoriaListener();
        service.registrarListener(primeiro);
        service.registrarListener(segundo);

        ResultadoPagamento resultado = service.processar(pix("200.00"));

        assertThat(primeiro.getEventos()).singleElement()
                .satisfies(evento -> {
                    assertThat(evento.pagamentoId()).isEqualTo(resultado.pagamentoId());
                    assertThat(evento.status()).isEqualTo(StatusPagamento.APROVADO);
                });
        assertThat(segundo.getEventos()).hasSize(1);
    }

    @Test
    void deveNotificarTambemQuandoAValidacaoRecusa() {
        AuditoriaListener auditoria = new AuditoriaListener();
        service.registrarListener(auditoria);

        service.processar(pix("1000.01"));

        assertThat(auditoria.getEventos()).singleElement()
                .satisfies(evento -> {
                    assertThat(evento.status()).isEqualTo(StatusPagamento.RECUSADO);
                    assertThat(evento.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
                });
    }

    @Test
    void falhaDeUmListenerNaoDerrubaOFluxoNemImpedeOsDemais() {
        List<String> chamadas = new ArrayList<>();
        AuditoriaListener depoisDaFalha = new AuditoriaListener();
        service.registrarListener((pagamento, resultado) -> chamadas.add("antes"));
        service.registrarListener((pagamento, resultado) -> {
            throw new IllegalStateException("servidor de e-mail fora do ar");
        });
        service.registrarListener(depoisDaFalha);

        ResultadoPagamento resultado = service.processar(pix("200.00"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(chamadas).containsExactly("antes");
        assertThat(depoisDaFalha.getEventos()).hasSize(1);
    }

    @Test
    void naoDeveAceitarListenerNulo() {
        PagamentoListener nulo = null;

        assertThatThrownBy(() -> service.registrarListener(nulo)).isInstanceOf(NullPointerException.class);
    }
}
