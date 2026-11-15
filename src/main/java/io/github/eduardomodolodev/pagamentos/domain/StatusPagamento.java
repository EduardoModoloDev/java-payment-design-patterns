package io.github.eduardomodolodev.pagamentos.domain;

/**
 * Ciclo de vida de um pagamento.
 *
 * <ul>
 *     <li>{@link #CRIADO}: recebido, ainda não processado;</li>
 *     <li>{@link #PENDENTE}: aguardando confirmação (ex.: boleto gerado);</li>
 *     <li>{@link #APROVADO}: valor debitado com sucesso;</li>
 *     <li>{@link #RECUSADO}: barrado por alguma regra de negócio.</li>
 * </ul>
 */
public enum StatusPagamento {
    CRIADO,
    PENDENTE,
    APROVADO,
    RECUSADO
}
