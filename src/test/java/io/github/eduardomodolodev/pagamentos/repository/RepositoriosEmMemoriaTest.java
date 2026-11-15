package io.github.eduardomodolodev.pagamentos.repository;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoriosEmMemoriaTest {

    private static Pagamento pagamento(String contaId, String valor) {
        return Pagamento.builder()
                .contaOrigemId(contaId)
                .tipo(TipoPagamento.PIX)
                .valor(new BigDecimal(valor))
                .destinatario("chave")
                .build();
    }

    @Test
    void deveSalvarEBuscarContaPorId() {
        ContaRepository repositorio = new ContaRepositoryEmMemoria();
        Conta conta = new Conta("CONTA-001", "Maria", BigDecimal.TEN, BigDecimal.TEN);

        repositorio.salvar(conta);

        assertThat(repositorio.buscarPorId("CONTA-001")).containsSame(conta);
        assertThat(repositorio.buscarPorId("CONTA-999")).isEmpty();
        assertThat(repositorio.buscarPorId(null)).isEmpty();
    }

    @Test
    void deveSalvarEBuscarPagamentoPorId() {
        PagamentoRepository repositorio = new PagamentoRepositoryEmMemoria();
        Pagamento pagamento = pagamento("CONTA-001", "10");

        repositorio.salvar(pagamento);

        assertThat(repositorio.buscarPorId(pagamento.getId())).containsSame(pagamento);
        assertThat(repositorio.buscarPorId("inexistente")).isEmpty();
    }

    @Test
    void deveListarPagamentosDaContaNaOrdemDeInsercao() {
        PagamentoRepository repositorio = new PagamentoRepositoryEmMemoria();
        Pagamento primeiro = pagamento("CONTA-001", "10");
        Pagamento deOutraConta = pagamento("CONTA-002", "20");
        Pagamento segundo = pagamento("CONTA-001", "30");

        repositorio.salvar(primeiro);
        repositorio.salvar(deOutraConta);
        repositorio.salvar(segundo);

        assertThat(repositorio.listarPorConta("CONTA-001")).containsExactly(primeiro, segundo);
        assertThat(repositorio.listarPorConta("CONTA-003")).isEmpty();
    }

    @Test
    void salvarDeNovoOMesmoPagamentoNaoDuplicaOHistorico() {
        PagamentoRepository repositorio = new PagamentoRepositoryEmMemoria();
        Pagamento pagamento = pagamento("CONTA-001", "10");

        repositorio.salvar(pagamento);
        repositorio.salvar(pagamento);

        assertThat(repositorio.listarPorConta("CONTA-001")).hasSize(1);
    }
}
