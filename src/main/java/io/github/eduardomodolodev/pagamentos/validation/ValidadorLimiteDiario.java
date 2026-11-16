package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Limite diário da conta (não se aplica a cartão de crédito).
 *
 * <p>A soma dos pagamentos {@code APROVADO} do dia (Pix e boleto confirmado)
 * mais o valor atual não pode exceder {@code conta.limiteDiario}
 * ({@code LIMITE_DIARIO_EXCEDIDO}). O "dia" é o da criação do pagamento atual.
 */
public class ValidadorLimiteDiario extends Validador {

    private final PagamentoRepository pagamentoRepository;

    public ValidadorLimiteDiario(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = Objects.requireNonNull(pagamentoRepository, "pagamentoRepository");
    }

    @Override
    protected void verificar(Pagamento pagamento, Conta conta) {
        if (pagamento.getTipo() == TipoPagamento.CARTAO_CREDITO) {
            return;
        }

        LocalDate dia = pagamento.getCriadoEm().toLocalDate();
        BigDecimal aprovadoNoDia = pagamentoRepository.listarPorConta(conta.getId()).stream()
                .filter(outro -> !outro.getId().equals(pagamento.getId()))
                .filter(outro -> outro.getStatus() == StatusPagamento.APROVADO)
                .filter(outro -> outro.getTipo() != TipoPagamento.CARTAO_CREDITO)
                .filter(outro -> outro.getCriadoEm().toLocalDate().equals(dia))
                .map(Pagamento::getValor)
                .reduce(Dinheiro.ZERO, BigDecimal::add);

        BigDecimal totalComAtual = aprovadoNoDia.add(pagamento.getValor());
        if (totalComAtual.compareTo(conta.getLimiteDiario()) > 0) {
            throw new ValidacaoException(CodigoRecusa.LIMITE_DIARIO_EXCEDIDO,
                    "Limite diário de %s excedido: já aprovado hoje %s, valor atual %s"
                            .formatted(conta.getLimiteDiario(), aprovadoNoDia, pagamento.getValor()));
        }
    }
}
