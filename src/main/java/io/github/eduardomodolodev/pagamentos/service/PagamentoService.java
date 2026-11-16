package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.repository.ContaRepository;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;
import io.github.eduardomodolodev.pagamentos.validation.ValidacaoException;
import io.github.eduardomodolodev.pagamentos.validation.Validador;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>Padrão Facade:</b> ponto único de entrada do simulador. Quem chama não
 * precisa conhecer repositórios, validadores, fábrica nem processadores — só
 * pede para processar um pagamento.
 *
 * <p>Fluxo de {@link #processar(Pagamento)}:
 * <ol>
 *     <li>busca a conta (erro claro se não existir);</li>
 *     <li>salva o pagamento com status {@code CRIADO};</li>
 *     <li>executa a cadeia de validações; uma recusa vira resultado
 *         {@code RECUSADO} (não vaza como exceção);</li>
 *     <li>pede o processador à {@link ProcessadorFactory};</li>
 *     <li>processa, persiste conta e pagamento e devolve o resultado.</li>
 * </ol>
 */
public class PagamentoService {

    private final ContaRepository contaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ProcessadorFactory processadorFactory;
    private final Validador validacoes;
    private final Clock clock;

    public PagamentoService(ContaRepository contaRepository,
                            PagamentoRepository pagamentoRepository,
                            ProcessadorFactory processadorFactory,
                            Validador validacoes,
                            Clock clock) {
        this.contaRepository = Objects.requireNonNull(contaRepository, "contaRepository");
        this.pagamentoRepository = Objects.requireNonNull(pagamentoRepository, "pagamentoRepository");
        this.processadorFactory = Objects.requireNonNull(processadorFactory, "processadorFactory");
        this.validacoes = Objects.requireNonNull(validacoes, "validacoes");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Processa um pagamento. Recusas de negócio voltam como resultado
     * {@code RECUSADO}, com o código no campo {@code codigo}.
     *
     * @throws ContaNaoEncontradaException se a conta de origem não existir
     */
    public ResultadoPagamento processar(Pagamento pagamento) {
        Objects.requireNonNull(pagamento, "pagamento não pode ser nulo");
        Conta conta = contaRepository.buscarPorId(pagamento.getContaOrigemId())
                .orElseThrow(() -> new ContaNaoEncontradaException(pagamento.getContaOrigemId()));

        pagamentoRepository.salvar(pagamento);

        try {
            validacoes.validar(pagamento, conta);
        } catch (ValidacaoException recusa) {
            return recusar(pagamento, recusa);
        }

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

    private ResultadoPagamento recusar(Pagamento pagamento, ValidacaoException recusa) {
        pagamento.atualizarStatus(StatusPagamento.RECUSADO);
        pagamentoRepository.salvar(pagamento);
        return ResultadoPagamento.de(pagamento, recusa.getCodigo(), recusa.getMessage(), LocalDateTime.now(clock));
    }
}
