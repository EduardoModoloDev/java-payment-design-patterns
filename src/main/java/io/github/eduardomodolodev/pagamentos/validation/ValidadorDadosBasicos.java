package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Primeiro elo da cadeia (o mais barato): confere os dados do próprio pagamento.
 *
 * <ul>
 *     <li>tipo informado, valor maior que zero e destinatário preenchido
 *         ({@code DADOS_INVALIDOS});</li>
 *     <li>cartão com parcelas entre 1 e 12 ({@code PARCELAS_INVALIDAS});</li>
 *     <li>boleto com vencimento informado ({@code DADOS_INVALIDOS}) e que não
 *         esteja no passado ({@code BOLETO_VENCIDO}).</li>
 * </ul>
 */
public class ValidadorDadosBasicos extends Validador {

    public static final int PARCELAS_MINIMAS = 1;
    public static final int PARCELAS_MAXIMAS = 12;

    private final Clock clock;

    public ValidadorDadosBasicos(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    @Override
    protected void verificar(Pagamento pagamento, Conta conta) {
        if (pagamento.getTipo() == null) {
            throw new ValidacaoException(CodigoRecusa.DADOS_INVALIDOS, "Tipo de pagamento não informado");
        }
        if (pagamento.getValor() == null || pagamento.getValor().signum() <= 0) {
            throw new ValidacaoException(CodigoRecusa.DADOS_INVALIDOS, "O valor deve ser maior que zero");
        }
        if (pagamento.getDestinatario() == null || pagamento.getDestinatario().isBlank()) {
            throw new ValidacaoException(CodigoRecusa.DADOS_INVALIDOS, "Destinatário não informado");
        }

        if (pagamento.getTipo() == TipoPagamento.CARTAO_CREDITO) {
            verificarParcelas(pagamento.getParcelas());
        } else if (pagamento.getTipo() == TipoPagamento.BOLETO) {
            verificarVencimento(pagamento.getDataVencimento());
        }
    }

    private void verificarParcelas(int parcelas) {
        if (parcelas < PARCELAS_MINIMAS || parcelas > PARCELAS_MAXIMAS) {
            throw new ValidacaoException(CodigoRecusa.PARCELAS_INVALIDAS,
                    "Parcelamento deve ser de %d a %dx, recebido: %dx"
                            .formatted(PARCELAS_MINIMAS, PARCELAS_MAXIMAS, parcelas));
        }
    }

    private void verificarVencimento(LocalDate dataVencimento) {
        if (dataVencimento == null) {
            throw new ValidacaoException(CodigoRecusa.DADOS_INVALIDOS, "Boleto sem data de vencimento");
        }
        LocalDate hoje = LocalDate.now(clock);
        if (dataVencimento.isBefore(hoje)) {
            throw new ValidacaoException(CodigoRecusa.BOLETO_VENCIDO,
                    "Boleto vencido em " + dataVencimento);
        }
    }
}
