package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.observer.AuditoriaListener;
import io.github.eduardomodolodev.pagamentos.observer.SmsListener;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.validation.CadeiaValidadores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxos de ponta a ponta dos 10 cenários da seção 13 da especificação,
 * passando por todos os padrões: Facade → Chain → Factory → Decorator →
 * Strategy → Observer.
 */
class CenariosEspecificacaoTest {

    /** Relógio de teste que só anda quando mandamos. */
    private static final class RelogioControlavel extends Clock {
        private Instant agora;

        RelogioControlavel(LocalDateTime inicio) {
            this.agora = inicio.toInstant(ZoneOffset.UTC);
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        void ajustar(LocalDateTime momento) {
            agora = momento.toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(agora, zona);
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 17, 14, 0);

    private final RelogioControlavel relogio = new RelogioControlavel(INICIO);
    private final AuditoriaListener auditoria = new AuditoriaListener();
    private final List<String> sms = new ArrayList<>();
    private ContaRepository contas;
    private PagamentoService service;

    @BeforeEach
    void setUp() {
        contas = new ContaRepositoryEmMemoria();
        contas.salvar(new Conta("CONTA-001", "Maria", new BigDecimal("3000.00"), new BigDecimal("5000.00")));
        PagamentoRepository pagamentos = new PagamentoRepositoryEmMemoria();
        service = new PagamentoService(contas, pagamentos, new ProcessadorFactory(relogio),
                CadeiaValidadores.padrao(pagamentos, relogio), relogio);
        service.registrarListener(auditoria);
        service.registrarListener(new SmsListener(sms::add));
    }

    private Conta conta() {
        return contas.buscarPorId("CONTA-001").orElseThrow();
    }

    private Pagamento.Builder pagamento(TipoPagamento tipo, String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .destinatario("Favorecido")
                .clock(relogio);
    }

    private ResultadoPagamento pix(String valor) {
        return service.processar(pagamento(TipoPagamento.PIX, valor).build());
    }

    private ResultadoPagamento boleto(String valor, LocalDate vencimento) {
        return service.processar(pagamento(TipoPagamento.BOLETO, valor).dataVencimento(vencimento).build());
    }

    @Test
    @DisplayName("1 — Pix de R$ 200,00 com saldo suficiente: APROVADO, taxa 0")
    void cenario1() {
        ResultadoPagamento resultado = pix("200.00");

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.taxa()).isEqualByComparingTo("0");
        assertThat(conta().getSaldo()).isEqualByComparingTo("2800.00");
        assertThat(auditoria.getEventos()).hasSize(1);
        assertThat(sms).isEmpty();
    }

    @Test
    @DisplayName("2 — Pix de R$ 1.500,00 às 22h: LIMITE_NOTURNO_EXCEDIDO")
    void cenario2() {
        relogio.ajustar(INICIO.withHour(22));

        ResultadoPagamento resultado = pix("1500.00");

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.LIMITE_NOTURNO_EXCEDIDO);
        assertThat(conta().getSaldo()).isEqualByComparingTo("3000.00");
        assertThat(sms).hasSize(1);
    }

    @Test
    @DisplayName("3 — Cartão R$ 1.200,00 em 6x: taxa 84, total 1.284, cashback 12")
    void cenario3() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.CARTAO_CREDITO, "1200.00").parcelas(6).build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.taxa()).isEqualByComparingTo(new BigDecimal("84.00"));
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("1284.00"));
        assertThat(resultado.cashback()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(conta().getLimiteCartaoDisponivel()).isEqualByComparingTo("3716.00");
        assertThat(conta().getSaldo()).isEqualByComparingTo("3000.00");
        assertThat(sms).hasSize(1);
    }

    @Test
    @DisplayName("4 — Cartão R$ 900,00 em 13x: PARCELAS_INVALIDAS")
    void cenario4() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").parcelas(13).build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.PARCELAS_INVALIDAS);
        assertThat(conta().getLimiteCartaoDisponivel()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("5 — Boleto R$ 350,00 com vencimento futuro: PENDENTE, taxa 3,50")
    void cenario5() {
        ResultadoPagamento resultado = boleto("350.00", INICIO.toLocalDate().plusDays(10));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(resultado.taxa()).isEqualByComparingTo("3.50");
        assertThat(conta().getSaldo()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("6 — confirmarBoleto do cenário 5: APROVADO e débito de R$ 353,50")
    void cenario6() {
        ResultadoPagamento pendente = boleto("350.00", INICIO.toLocalDate().plusDays(10));
        relogio.avancar(Duration.ofHours(1));

        ResultadoPagamento confirmado = service.confirmarBoleto(pendente.pagamentoId());

        assertThat(confirmado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(confirmado.total()).isEqualByComparingTo("353.50");
        assertThat(conta().getSaldo()).isEqualByComparingTo("2646.50");
        assertThat(auditoria.getEventos())
                .extracting(AuditoriaListener.Evento::status)
                .containsExactly(StatusPagamento.PENDENTE, StatusPagamento.APROVADO);
    }

    @Test
    @DisplayName("7 — Boleto vencido: BOLETO_VENCIDO")
    void cenario7() {
        ResultadoPagamento resultado = boleto("120.00", INICIO.toLocalDate().minusDays(1));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.BOLETO_VENCIDO);
    }

    @Test
    @DisplayName("8 — Pix maior que o saldo: SALDO_INSUFICIENTE")
    void cenario8() {
        ResultadoPagamento resultado = pix("3000.01");

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
        assertThat(conta().getSaldo()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("9 — 5 pagamentos seguidos em poucos segundos: o 6º é FRAUDE_VELOCIDADE")
    void cenario9() {
        for (int i = 1; i <= 5; i++) {
            relogio.avancar(Duration.ofSeconds(10));
            assertThat(pix("50.00").status()).as("pagamento %d", i).isEqualTo(StatusPagamento.APROVADO);
        }
        relogio.avancar(Duration.ofSeconds(10));

        ResultadoPagamento sexto = pix("50.00");

        assertThat(sexto.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(sexto.codigo()).isEqualTo(CodigoRecusa.FRAUDE_VELOCIDADE);
        assertThat(conta().getSaldo()).isEqualByComparingTo("2750.00");
    }

    @Test
    @DisplayName("10 — Pagamentos acumulados passando de R$ 5.000,00 no dia: LIMITE_DIARIO_EXCEDIDO")
    void cenario10() {
        contas.salvar(new Conta("CONTA-001", "Maria", new BigDecimal("10000.00"), new BigDecimal("5000.00")));

        relogio.avancar(Duration.ofMinutes(1));
        assertThat(pix("2000.00").status()).isEqualTo(StatusPagamento.APROVADO);
        relogio.avancar(Duration.ofMinutes(1));
        assertThat(pix("2000.00").status()).isEqualTo(StatusPagamento.APROVADO);
        relogio.avancar(Duration.ofMinutes(1));

        ResultadoPagamento terceiro = pix("1500.00");

        assertThat(terceiro.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(terceiro.codigo()).isEqualTo(CodigoRecusa.LIMITE_DIARIO_EXCEDIDO);
        assertThat(conta().getSaldo()).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("Histórico da conta guarda todos os pagamentos, inclusive recusados")
    void historicoCompleto() {
        pix("200.00");
        relogio.avancar(Duration.ofMinutes(1));
        service.processar(pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").parcelas(13).build());
        relogio.avancar(Duration.ofMinutes(1));
        boleto("350.00", INICIO.toLocalDate().plusDays(3));

        assertThat(service.listarPagamentos("CONTA-001"))
                .extracting(Pagamento::getStatus)
                .containsExactly(StatusPagamento.APROVADO, StatusPagamento.RECUSADO, StatusPagamento.PENDENTE);
        assertThat(auditoria.getEventos()).hasSize(3);
    }
}
