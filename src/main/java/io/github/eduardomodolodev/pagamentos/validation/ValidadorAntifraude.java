package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Último elo da cadeia (o mais caro, pois varre o histórico da conta).
 *
 * <ul>
 *     <li><b>Velocidade:</b> 5 ou mais pagamentos da conta nos últimos 2
 *         minutos ({@code FRAUDE_VELOCIDADE});</li>
 *     <li><b>Valor atípico:</b> conta com 3 ou mais pagamentos aprovados e valor
 *         atual maior que 5× a média dos aprovados ({@code FRAUDE_VALOR_ATIPICO}).</li>
 * </ul>
 *
 * <p>A janela de tempo é contada a partir da criação do pagamento atual.
 */
public class ValidadorAntifraude extends Validador {

    public static final int MAXIMO_PAGAMENTOS_NA_JANELA = 5;
    public static final Duration JANELA_VELOCIDADE = Duration.ofMinutes(2);
    public static final int MINIMO_APROVADOS_PARA_MEDIA = 3;
    public static final BigDecimal MULTIPLICADOR_VALOR_ATIPICO = new BigDecimal("5");

    private final PagamentoRepository pagamentoRepository;

    public ValidadorAntifraude(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = Objects.requireNonNull(pagamentoRepository, "pagamentoRepository");
    }

    @Override
    protected void verificar(Pagamento pagamento, Conta conta) {
        List<Pagamento> historico = pagamentoRepository.listarPorConta(conta.getId()).stream()
                .filter(outro -> !outro.getId().equals(pagamento.getId()))
                .toList();

        verificarVelocidade(pagamento, historico);
        verificarValorAtipico(pagamento, historico);
    }

    private void verificarVelocidade(Pagamento pagamento, List<Pagamento> historico) {
        LocalDateTime agora = pagamento.getCriadoEm();
        LocalDateTime inicioJanela = agora.minus(JANELA_VELOCIDADE);
        long recentes = historico.stream()
                .map(Pagamento::getCriadoEm)
                .filter(criadoEm -> !criadoEm.isBefore(inicioJanela) && !criadoEm.isAfter(agora))
                .count();

        if (recentes >= MAXIMO_PAGAMENTOS_NA_JANELA) {
            throw new ValidacaoException(CodigoRecusa.FRAUDE_VELOCIDADE,
                    "%d pagamentos nos últimos %d minutos: possível fraude"
                            .formatted(recentes, JANELA_VELOCIDADE.toMinutes()));
        }
    }

    private void verificarValorAtipico(Pagamento pagamento, List<Pagamento> historico) {
        List<BigDecimal> aprovados = historico.stream()
                .filter(outro -> outro.getStatus() == StatusPagamento.APROVADO)
                .map(Pagamento::getValor)
                .toList();
        if (aprovados.size() < MINIMO_APROVADOS_PARA_MEDIA) {
            return;
        }

        BigDecimal soma = aprovados.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal media = soma.divide(BigDecimal.valueOf(aprovados.size()), Dinheiro.ESCALA, Dinheiro.ARREDONDAMENTO);
        BigDecimal teto = media.multiply(MULTIPLICADOR_VALOR_ATIPICO);

        if (pagamento.getValor().compareTo(teto) > 0) {
            throw new ValidacaoException(CodigoRecusa.FRAUDE_VALOR_ATIPICO,
                    "Valor %s maior que 5x a média dos pagamentos aprovados (%s): possível fraude"
                            .formatted(pagamento.getValor(), media));
        }
    }
}
