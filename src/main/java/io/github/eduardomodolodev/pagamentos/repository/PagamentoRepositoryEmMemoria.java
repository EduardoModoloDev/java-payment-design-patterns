package io.github.eduardomodolodev.pagamentos.repository;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação de {@link PagamentoRepository} em memória.
 *
 * <p>Usa um {@link LinkedHashMap} para preservar a ordem de inserção no histórico.
 */
public class PagamentoRepositoryEmMemoria implements PagamentoRepository {

    private final Map<String, Pagamento> pagamentos = new LinkedHashMap<>();

    @Override
    public synchronized Pagamento salvar(Pagamento pagamento) {
        Objects.requireNonNull(pagamento, "pagamento não pode ser nulo");
        pagamentos.put(pagamento.getId(), pagamento);
        return pagamento;
    }

    @Override
    public synchronized Optional<Pagamento> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pagamentos.get(id));
    }

    @Override
    public synchronized List<Pagamento> listarPorConta(String contaId) {
        return pagamentos.values().stream()
                .filter(pagamento -> pagamento.getContaOrigemId().equals(contaId))
                .toList();
    }
}
