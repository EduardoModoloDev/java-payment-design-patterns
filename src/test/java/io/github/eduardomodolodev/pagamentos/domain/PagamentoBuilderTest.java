package io.github.eduardomodolodev.pagamentos.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoBuilderTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO =
            Clock.fixed(Instant.parse("2026-11-15T19:30:00Z"), ZONA);

    private Pagamento.Builder builderValido() {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal("200"))
                .destinatario("chave@pix.com")
                .clock(RELOGIO_FIXO);
    }

    @Test
    void deveCriarPagamentoComCamposObrigatorios() {
        Pagamento pagamento = builderValido().build();

        assertThat(pagamento.getContaOrigemId()).isEqualTo("CONTA-001");
        assertThat(pagamento.getTipo()).isEqualTo(TipoPagamento.PIX);
        assertThat(pagamento.getValor()).isEqualTo(new BigDecimal("200.00"));
        assertThat(pagamento.getDestinatario()).isEqualTo("chave@pix.com");
    }

    @Test
    void deveAplicarValoresPadrao() {
        Pagamento pagamento = builderValido().build();

        assertThat(pagamento.getParcelas()).isEqualTo(1);
        assertThat(pagamento.getDataVencimento()).isNull();
        assertThat(pagamento.getTaxa()).isEqualByComparingTo("0");
        assertThat(pagamento.getCashback()).isEqualByComparingTo("0");
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CRIADO);
        assertThat(pagamento.getValorTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void deveGerarIdUuidUnico() {
        Pagamento primeiro = builderValido().build();
        Pagamento segundo = builderValido().build();

        assertThat(UUID.fromString(primeiro.getId())).isNotNull();
        assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
    }

    @Test
    void deveGerarCriadoEmAPartirDoClock() {
        Pagamento pagamento = builderValido().build();

        assertThat(pagamento.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 11, 15, 16, 30));
    }

    @Test
    void deveAceitarCamposOpcionais() {
        Pagamento cartao = builderValido()
                .tipo(TipoPagamento.CARTAO_CREDITO)
                .parcelas(6)
                .build();
        Pagamento boleto = builderValido()
                .tipo(TipoPagamento.BOLETO)
                .dataVencimento(LocalDate.of(2026, 12, 1))
                .build();

        assertThat(cartao.getParcelas()).isEqualTo(6);
        assertThat(boleto.getDataVencimento()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void deveExigirContaOrigem() {
        assertThatThrownBy(() -> builderValido().contaOrigemId(null).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contaOrigemId");
        assertThatThrownBy(() -> builderValido().contaOrigemId("  ").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contaOrigemId");
    }

    @Test
    void deveExigirTipo() {
        assertThatThrownBy(() -> builderValido().tipo(null).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    void deveExigirValor() {
        assertThatThrownBy(() -> builderValido().valor(null).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valor");
    }

    @Test
    void deveExigirDestinatario() {
        assertThatThrownBy(() -> builderValido().destinatario(null).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("destinatario");
    }

    @Test
    void deveSomarTaxaECalcularValorTotal() {
        Pagamento pagamento = builderValido().valor(new BigDecimal("1200.00")).build();

        pagamento.adicionarTaxa(new BigDecimal("30.00"));
        pagamento.adicionarTaxa(new BigDecimal("54.00"));

        assertThat(pagamento.getTaxa()).isEqualByComparingTo("84.00");
        assertThat(pagamento.getValorTotal()).isEqualByComparingTo("1284.00");
    }

    @Test
    void deveAcumularCashback() {
        Pagamento pagamento = builderValido().build();

        pagamento.adicionarCashback(new BigDecimal("12.00"));

        assertThat(pagamento.getCashback()).isEqualByComparingTo("12.00");
    }

    @Test
    void naoDeveAceitarTaxaNegativa() {
        Pagamento pagamento = builderValido().build();

        assertThatThrownBy(() -> pagamento.adicionarTaxa(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
