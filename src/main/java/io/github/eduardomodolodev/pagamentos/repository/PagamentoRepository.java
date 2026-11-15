package io.github.eduardomodolodev.pagamentos.repository;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;

import java.util.List;
import java.util.Optional;

/**
 * Persistência de pagamentos (histórico por conta).
 */
public interface PagamentoRepository {

    Pagamento salvar(Pagamento pagamento);

    Optional<Pagamento> buscarPorId(String id);

    /** Pagamentos da conta, na ordem em que foram registrados. */
    List<Pagamento> listarPorConta(String contaId);
}
