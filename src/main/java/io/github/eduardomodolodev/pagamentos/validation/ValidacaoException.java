package io.github.eduardomodolodev.pagamentos.validation;

/**
 * Recusa de negócio levantada por um {@link Validador}.
 *
 * <p>Carrega um código estável (ver {@code CodigoRecusa}) que vai para o
 * {@code ResultadoPagamento}. O {@code PagamentoService} captura esta exceção
 * e a converte em resultado {@code RECUSADO}: recusa de negócio não é erro de
 * sistema e não vaza para quem chama.
 */
public class ValidacaoException extends RuntimeException {

    private final String codigo;

    public ValidacaoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
