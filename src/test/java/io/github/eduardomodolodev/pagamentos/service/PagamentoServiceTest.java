package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.validation.CadeiaValidadores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 14, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private ContaRepository contaRepository;
    private PagamentoService service;

    @BeforeEach
    void setUp() {
        contaRepository = new ContaRepositoryEmMemoria();
        contaRepository.salvar(new Conta("CONTA-001", "Maria", new BigDecimal("2000.00"), new BigDecimal("3000.00")));
        PagamentoRepository pagamentoRepository = new PagamentoRepositoryEmMemoria();
        service = new PagamentoService(contaRepository, pagamentoRepository, new ProcessadorFactory(RELOGIO),
                CadeiaValidadores.padrao(pagamentoRepository, RELOGIO), RELOGIO);
    }

    private Conta conta() {
        return contaRepository.buscarPorId("CONTA-001").orElseThrow();
    }

    private static Pagamento.Builder pagamento(TipoPagamento tipo, String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .destinatario("Favorecido")
                .clock(RELOGIO);
    }

    @Test
    void deveProcessarPixDebitandoOSaldo() {
        ResultadoPagamento resultado = service.processar(pagamento(TipoPagamento.PIX, "200.00").build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.taxa()).isEqualByComparingTo("0");
        assertThat(conta().getSaldo()).isEqualByComparingTo("1800.00");
    }

    @Test
    void deveProcessarCartaoDebitandoOLimite() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.CARTAO_CREDITO, "1200.00").parcelas(6).build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(conta().getSaldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void deveDeixarBoletoPendenteSemDebitar() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(AGORA.toLocalDate().plusDays(5)).build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(conta().getSaldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void devePersistirOPagamentoComStatusFinal() {
        Pagamento pagamento = pagamento(TipoPagamento.PIX, "50.00").build();

        service.processar(pagamento);

        assertThat(service.buscarPagamento(pagamento.getId()))
                .get()
                .extracting(Pagamento::getStatus)
                .isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void deveListarOHistoricoDaConta() {
        Pagamento primeiro = pagamento(TipoPagamento.PIX, "10.00").build();
        Pagamento segundo = pagamento(TipoPagamento.CARTAO_CREDITO, "20.00").build();

        service.processar(primeiro);
        service.processar(segundo);

        assertThat(service.listarPagamentos("CONTA-001")).containsExactly(primeiro, segundo);
    }

    @Test
    void deveConverterRecusaDaValidacaoEmResultadoSemLancarExcecao() {
        Pagamento pagamento = pagamento(TipoPagamento.PIX, "2000.01").build();

        ResultadoPagamento resultado = service.processar(pagamento);

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
        assertThat(resultado.mensagem()).contains("Saldo insuficiente");
        assertThat(resultado.processadoEm()).isEqualTo(AGORA);
        assertThat(conta().getSaldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void devePersistirOPagamentoRecusadoNoHistorico() {
        Pagamento pagamento = pagamento(TipoPagamento.CARTAO_CREDITO, "900.00").parcelas(13).build();

        ResultadoPagamento resultado = service.processar(pagamento);

        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.PARCELAS_INVALIDAS);
        assertThat(service.buscarPagamento(pagamento.getId()))
                .get()
                .extracting(Pagamento::getStatus)
                .isEqualTo(StatusPagamento.RECUSADO);
        assertThat(conta().getLimiteCartaoDisponivel()).isEqualByComparingTo("3000.00");
    }

    @Test
    void deveRecusarBoletoVencido() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.BOLETO, "100.00").dataVencimento(LocalDate.of(2026, 11, 15)).build());

        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.BOLETO_VENCIDO);
    }

    private Pagamento boletoPendente(String valor) {
        Pagamento boleto = pagamento(TipoPagamento.BOLETO, valor)
                .dataVencimento(AGORA.toLocalDate().plusDays(10))
                .build();
        assertThat(service.processar(boleto).status()).isEqualTo(StatusPagamento.PENDENTE);
        return boleto;
    }

    @Test
    void confirmarBoletoDeveDebitarValorMaisTaxaEAprovar() {
        Pagamento boleto = boletoPendente("350.00");

        ResultadoPagamento resultado = service.confirmarBoleto(boleto.getId());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.codigo()).isNull();
        assertThat(resultado.total()).isEqualByComparingTo("353.50");
        assertThat(conta().getSaldo()).isEqualByComparingTo("1646.50");
        assertThat(service.buscarPagamento(boleto.getId()))
                .get().extracting(Pagamento::getStatus).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void confirmarBoletoDeveRecusarQuandoOSaldoNaoCobreValorMaisTaxa() {
        Pagamento boleto = boletoPendente("1999.00");
        conta().debitarSaldo(new BigDecimal("1.00")); // saldo 1999,00 < 1999,00 + 3,50

        ResultadoPagamento resultado = service.confirmarBoleto(boleto.getId());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(resultado.codigo()).isEqualTo(CodigoRecusa.SALDO_INSUFICIENTE);
        assertThat(conta().getSaldo()).isEqualByComparingTo("1999.00");
    }

    @Test
    void naoDeveConfirmarOMesmoBoletoDuasVezes() {
        Pagamento boleto = boletoPendente("100.00");
        service.confirmarBoleto(boleto.getId());

        assertThatThrownBy(() -> service.confirmarBoleto(boleto.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDENTE");
        assertThat(conta().getSaldo()).isEqualByComparingTo("1896.50");
    }

    @Test
    void naoDeveConfirmarPagamentoQueNaoEBoleto() {
        Pagamento pix = pagamento(TipoPagamento.PIX, "10.00").build();
        service.processar(pix);

        assertThatThrownBy(() -> service.confirmarBoleto(pix.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmarBoletoInexistenteDeveLancarErroClaro() {
        assertThatThrownBy(() -> service.confirmarBoleto("nao-existe"))
                .isInstanceOf(PagamentoNaoEncontradoException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    void deveLancarErroClaroQuandoAContaNaoExiste() {
        Pagamento pagamento = pagamento(TipoPagamento.PIX, "10.00").contaOrigemId("CONTA-999").build();

        assertThatThrownBy(() -> service.processar(pagamento))
                .isInstanceOf(ContaNaoEncontradaException.class)
                .hasMessageContaining("CONTA-999");
        assertThat(service.buscarPagamento(pagamento.getId())).isEmpty();
    }
}
