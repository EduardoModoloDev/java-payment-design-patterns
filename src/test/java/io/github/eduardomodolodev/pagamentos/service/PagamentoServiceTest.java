package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepositoryEmMemoria;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoServiceTest {

    private ContaRepository contaRepository;
    private PagamentoService service;

    @BeforeEach
    void setUp() {
        contaRepository = new ContaRepositoryEmMemoria();
        contaRepository.salvar(new Conta("CONTA-001", "Maria", new BigDecimal("2000.00"), new BigDecimal("3000.00")));
        service = new PagamentoService(contaRepository, new PagamentoRepositoryEmMemoria(), new ProcessadorFactory());
    }

    private Conta conta() {
        return contaRepository.buscarPorId("CONTA-001").orElseThrow();
    }

    private static Pagamento.Builder pagamento(TipoPagamento tipo, String valor) {
        return Pagamento.builder()
                .contaOrigemId("CONTA-001")
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .destinatario("Favorecido");
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
        assertThat(conta().getLimiteCartaoDisponivel()).isEqualByComparingTo("1800.00");
        assertThat(conta().getSaldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void deveDeixarBoletoPendenteSemDebitar() {
        ResultadoPagamento resultado = service.processar(
                pagamento(TipoPagamento.BOLETO, "350.00").dataVencimento(LocalDate.now().plusDays(5)).build());

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
    void deveDevolverRecusaDoProcessadorSemLancarExcecao() {
        ResultadoPagamento resultado = service.processar(pagamento(TipoPagamento.PIX, "2000.01").build());

        assertThat(resultado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(conta().getSaldo()).isEqualByComparingTo("2000.00");
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
