package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>Padrão Facade:</b> ponto único de entrada do simulador. Quem chama não
 * precisa conhecer repositórios, fábrica nem processadores — só pede para
 * processar um pagamento.
 *
 * <p>Fluxo atual (fase 1, ainda sem validações):
 * <ol>
 *     <li>busca a conta (erro claro se não existir);</li>
 *     <li>salva o pagamento com status {@code CRIADO};</li>
 *     <li>pede o processador à {@link ProcessadorFactory};</li>
 *     <li>processa, persiste conta e pagamento e devolve o resultado.</li>
 * </ol>
 */
public class PagamentoService {

    private final ContaRepository contaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ProcessadorFactory processadorFactory;

    public PagamentoService(ContaRepository contaRepository,
                            PagamentoRepository pagamentoRepository,
                            ProcessadorFactory processadorFactory) {
        this.contaRepository = Objects.requireNonNull(contaRepository, "contaRepository");
        this.pagamentoRepository = Objects.requireNonNull(pagamentoRepository, "pagamentoRepository");
        this.processadorFactory = Objects.requireNonNull(processadorFactory, "processadorFactory");
    }

    /**
     * Processa um pagamento.
     *
     * @throws ContaNaoEncontradaException se a conta de origem não existir
     */
    public ResultadoPagamento processar(Pagamento pagamento) {
        Objects.requireNonNull(pagamento, "pagamento não pode ser nulo");
        Conta conta = contaRepository.buscarPorId(pagamento.getContaOrigemId())
                .orElseThrow(() -> new ContaNaoEncontradaException(pagamento.getContaOrigemId()));

        pagamentoRepository.salvar(pagamento);

        ProcessadorPagamento processador = processadorFactory.criar(pagamento.getTipo());
        ResultadoPagamento resultado = processador.processar(pagamento, conta);

        contaRepository.salvar(conta);
        pagamentoRepository.salvar(pagamento);
        return resultado;
    }

    public Optional<Pagamento> buscarPagamento(String id) {
        return pagamentoRepository.buscarPorId(id);
    }

    public List<Pagamento> listarPagamentos(String contaId) {
        return pagamentoRepository.listarPorConta(contaId);
    }
}
