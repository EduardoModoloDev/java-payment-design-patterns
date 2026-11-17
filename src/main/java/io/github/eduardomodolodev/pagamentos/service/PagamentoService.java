package io.github.eduardomodolodev.pagamentos.service;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.factory.ProcessadorFactory;
import io.github.eduardomodolodev.pagamentos.observer.PagamentoListener;
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
import java.util.concurrent.CopyOnWriteArrayList;

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
 *     <li>processa e persiste conta e pagamento;</li>
 *     <li>notifica os {@link PagamentoListener} (Observer) e devolve o resultado.</li>
 * </ol>
 */
public class PagamentoService {

    private static final System.Logger LOGGER = System.getLogger(PagamentoService.class.getName());

    private final List<PagamentoListener> listeners = new CopyOnWriteArrayList<>();
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
        notificar(pagamento, resultado);
        return resultado;
    }

    /**
     * Simula o pagamento de um boleto pendente: reconfere o saldo contra
     * {@code valor + taxa}; se houver saldo, debita e aprova, senão recusa com
     * {@code SALDO_INSUFICIENTE}. Em ambos os casos os listeners são avisados.
     *
     * @throws PagamentoNaoEncontradoException se o pagamento não existir
     * @throws IllegalStateException           se o pagamento não for um boleto pendente
     */
    public ResultadoPagamento confirmarBoleto(String pagamentoId) {
        Pagamento boleto = pagamentoRepository.buscarPorId(pagamentoId)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
        if (boleto.getTipo() != TipoPagamento.BOLETO || boleto.getStatus() != StatusPagamento.PENDENTE) {
            throw new IllegalStateException("Só é possível confirmar boleto PENDENTE; pagamento %s é %s %s"
                    .formatted(pagamentoId, boleto.getTipo(), boleto.getStatus()));
        }
        Conta conta = contaRepository.buscarPorId(boleto.getContaOrigemId())
                .orElseThrow(() -> new ContaNaoEncontradaException(boleto.getContaOrigemId()));

        ResultadoPagamento resultado;
        if (conta.temSaldoPara(boleto.getValorTotal())) {
            conta.debitarSaldo(boleto.getValorTotal());
            boleto.atualizarStatus(StatusPagamento.APROVADO);
            resultado = ResultadoPagamento.de(boleto, null, "Boleto pago", LocalDateTime.now(clock));
        } else {
            boleto.atualizarStatus(StatusPagamento.RECUSADO);
            resultado = ResultadoPagamento.de(boleto, CodigoRecusa.SALDO_INSUFICIENTE,
                    "Saldo insuficiente para pagar o boleto: disponível %s, total %s"
                            .formatted(conta.getSaldo(), boleto.getValorTotal()),
                    LocalDateTime.now(clock));
        }

        contaRepository.salvar(conta);
        pagamentoRepository.salvar(boleto);
        notificar(boleto, resultado);
        return resultado;
    }

    /** Registra um ouvinte, avisado ao final de cada pagamento. */
    public void registrarListener(PagamentoListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener não pode ser nulo"));
    }

    /**
     * Consulta uma conta (saldo, limites e dados do titular).
     *
     * @throws ContaNaoEncontradaException se a conta não existir
     */
    public Conta buscarConta(String contaId) {
        return contaRepository.buscarPorId(contaId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contaId));
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
        ResultadoPagamento resultado = ResultadoPagamento.de(
                pagamento, recusa.getCodigo(), recusa.getMessage(), LocalDateTime.now(clock));
        notificar(pagamento, resultado);
        return resultado;
    }

    /**
     * Avisa todos os ouvintes. A falha de um deles é registrada e ignorada:
     * não derruba o fluxo nem impede que os demais sejam avisados.
     */
    private void notificar(Pagamento pagamento, ResultadoPagamento resultado) {
        for (PagamentoListener listener : listeners) {
            try {
                listener.aoFinalizar(pagamento, resultado);
            } catch (RuntimeException falha) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Listener %s falhou ao ser notificado do pagamento %s: %s"
                                .formatted(listener.getClass().getSimpleName(), pagamento.getId(), falha.getMessage()));
            }
        }
    }
}
