package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Simula o envio de um e-mail para <b>todo</b> resultado de pagamento.
 * Nenhum e-mail real é enviado: a mensagem vai para o console (ou para a
 * saída injetada).
 */
public class EmailListener implements PagamentoListener {

    private final Consumer<String> saida;

    public EmailListener() {
        this(System.out::println);
    }

    public EmailListener(Consumer<String> saida) {
        this.saida = Objects.requireNonNull(saida, "saida não pode ser nula");
    }

    @Override
    public void aoFinalizar(Pagamento pagamento, ResultadoPagamento resultado) {
        String detalhe = resultado.codigo() == null ? "" : " (" + resultado.codigo() + ")";
        saida.accept("[EMAIL] Conta %s: seu %s de %s para '%s' está %s%s. Total: %s.".formatted(
                pagamento.getContaOrigemId(), pagamento.getTipo(), resultado.valor(),
                pagamento.getDestinatario(), resultado.status(), detalhe, resultado.total()));
    }
}
