package io.github.eduardomodolodev.pagamentos.repository;

import io.github.eduardomodolodev.pagamentos.domain.Conta;

import java.util.Optional;

/**
 * Persistência de contas. A regra de negócio depende só desta interface,
 * então a implementação em memória pode ser trocada (ex.: JPA) sem impacto.
 */
public interface ContaRepository {

    Optional<Conta> buscarPorId(String id);

    Conta salvar(Conta conta);
}
