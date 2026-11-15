package io.github.eduardomodolodev.pagamentos.repository;

import io.github.eduardomodolodev.pagamentos.domain.Conta;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação de {@link ContaRepository} em memória.
 */
public class ContaRepositoryEmMemoria implements ContaRepository {

    private final Map<String, Conta> contas = new ConcurrentHashMap<>();

    @Override
    public Optional<Conta> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(contas.get(id));
    }

    @Override
    public Conta salvar(Conta conta) {
        Objects.requireNonNull(conta, "conta não pode ser nula");
        contas.put(conta.getId(), conta);
        return conta;
    }
}
