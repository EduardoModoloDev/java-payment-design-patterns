package io.github.eduardomodolodev.pagamentos.domain;

/**
 * Códigos estáveis de recusa, devolvidos em {@link ResultadoPagamento#codigo()}.
 *
 * <p>São {@code String} (e não enum) porque fazem parte do contrato externo do
 * resultado: novos validadores podem introduzir códigos sem alterar esta classe.
 */
public final class CodigoRecusa {

    public static final String DADOS_INVALIDOS = "DADOS_INVALIDOS";
    public static final String PARCELAS_INVALIDAS = "PARCELAS_INVALIDAS";
    public static final String BOLETO_VENCIDO = "BOLETO_VENCIDO";
    public static final String LIMITE_NOTURNO_EXCEDIDO = "LIMITE_NOTURNO_EXCEDIDO";
    public static final String SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    public static final String LIMITE_CARTAO_INSUFICIENTE = "LIMITE_CARTAO_INSUFICIENTE";
    public static final String LIMITE_DIARIO_EXCEDIDO = "LIMITE_DIARIO_EXCEDIDO";
    public static final String FRAUDE_VELOCIDADE = "FRAUDE_VELOCIDADE";
    public static final String FRAUDE_VALOR_ATIPICO = "FRAUDE_VALOR_ATIPICO";

    private CodigoRecusa() {
    }
}
