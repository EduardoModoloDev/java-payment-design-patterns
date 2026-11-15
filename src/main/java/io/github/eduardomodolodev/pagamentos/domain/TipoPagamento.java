package io.github.eduardomodolodev.pagamentos.domain;

/**
 * Meios de pagamento suportados pelo simulador.
 *
 * <p>Para adicionar um novo meio de pagamento basta incluir a constante aqui,
 * criar o processador correspondente e registrá-lo na {@code ProcessadorFactory}.
 */
public enum TipoPagamento {
    PIX,
    CARTAO_CREDITO,
    BOLETO
}
