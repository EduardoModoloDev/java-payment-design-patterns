package io.github.eduardomodolodev.pagamentos.decorator;

import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.strategy.ProcessadorPagamento;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Registra a entrada e a saída do processamento: tipo, valor, status, total e
 * tempo gasto. Por padrão escreve no console; a saída é injetável para testes.
 */
public class LogDecorator extends ProcessadorDecorator {

    private final Consumer<String> saida;

    public LogDecorator(ProcessadorPagamento delegado) {
        this(System.out::println, delegado);
    }

    public LogDecorator(Consumer<String> saida, ProcessadorPagamento delegado) {
        super(delegado);
        this.saida = Objects.requireNonNull(saida, "saida não pode ser nula");
    }

    @Override
    public ResultadoPagamento processar(Pagamento pagamento, Conta conta) {
        saida.accept("[LOG] >> %s de %s para '%s' (conta %s, pagamento %s)".formatted(
                pagamento.getTipo(), pagamento.getValor(), pagamento.getDestinatario(),
                conta.getId(), pagamento.getId()));

        long inicio = System.nanoTime();
        ResultadoPagamento resultado = delegado.processar(pagamento, conta);
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

        saida.accept("[LOG] << %s %s: valor %s, taxa %s, total %s, cashback %s em %d ms".formatted(
                pagamento.getTipo(), resultado.status(), resultado.valor(), resultado.taxa(),
                resultado.total(), resultado.cashback(), duracaoMs));
        return resultado;
    }
}
