package io.github.eduardomodolodev.pagamentos.service;

/**
 * Lançada quando uma operação referencia um pagamento que não existe.
 */
public class PagamentoNaoEncontradoException extends RuntimeException {

    private final String pagamentoId;

    public PagamentoNaoEncontradoException(String pagamentoId) {
        super("Pagamento não encontrado: " + pagamentoId);
        this.pagamentoId = pagamentoId;
    }

    public String getPagamentoId() {
        return pagamentoId;
    }
}
