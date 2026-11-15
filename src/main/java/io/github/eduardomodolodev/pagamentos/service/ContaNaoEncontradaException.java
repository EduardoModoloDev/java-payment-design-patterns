package io.github.eduardomodolodev.pagamentos.service;

/**
 * Lançada quando o pagamento referencia uma conta que não existe.
 *
 * <p>Diferente de uma recusa de negócio, é um erro de uso: quem chama
 * informou um identificador inválido.
 */
public class ContaNaoEncontradaException extends RuntimeException {

    private final String contaId;

    public ContaNaoEncontradaException(String contaId) {
        super("Conta não encontrada: " + contaId);
        this.contaId = contaId;
    }

    public String getContaId() {
        return contaId;
    }
}
