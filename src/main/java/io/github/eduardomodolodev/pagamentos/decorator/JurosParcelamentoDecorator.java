package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Dinheiro;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Juros simples de parcelamento, somados à taxa antes de delegar.
 *
 * <p>Padrão: até 3x sem juros; acima disso, 1,5% por parcela além da 3ª,
 * ou seja, {@code (parcelas − 3) × 1,5% × valor}. Ex.: R$ 1.200,00 em 6x →
 * 4,5% = R$ 54,00.
 */
public class JurosParcelamentoDecorator extends ProcessadorDecorator {

    public static final int PARCELAS_SEM_JUROS_PADRAO = 3;
    public static final BigDecimal JUROS_POR_PARCELA_PADRAO = new BigDecimal("0.015");

    private final int parcelasSemJuros;
    private final BigDecimal jurosPorParcela;

    public JurosParcelamentoDecorator(ProcessadorPagamento delegado) {
        this(PARCELAS_SEM_JUROS_PADRAO, JUROS_POR_PARCELA_PADRAO, delegado);
    }

    public JurosParcelamentoDecorator(int parcelasSemJuros, BigDecimal jurosPorParcela,
                                      ProcessadorPagamento delegado) {
        super(delegado);
        if (parcelasSemJuros < 1) {
            throw new IllegalArgumentException("Parcelas sem juros deve ser ao menos 1");
        }
        this.parcelasSemJuros = parcelasSemJuros;
        this.jurosPorParcela = Objects.requireNonNull(jurosPorParcela, "jurosPorParcela não pode ser nulo");
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        int parcelasComJuros = pagamento.getParcelas() - parcelasSemJuros;
        if (parcelasComJuros > 0) {
            BigDecimal percentual = jurosPorParcela.multiply(BigDecimal.valueOf(parcelasComJuros));
            pagamento.adicionarTaxa(Dinheiro.de(pagamento.getValor().multiply(percentual)));
        }
        return delegado.processar(pagamento, conta);
    }
}
