package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.api.dto.ContaResponse;
import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoResponse;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de contas e do histórico de pagamentos de cada uma. */
@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final PagamentoService service;

    public ContaController(PagamentoService service) {
        this.service = service;
    }

    /** Saldo, limites e dados da conta. */
    @GetMapping("/{id}")
    public ContaResponse buscar(@PathVariable String id) {
        return ContaResponse.de(service.buscarConta(id));
    }

    /** Histórico de pagamentos da conta, na ordem em que foram feitos (inclui recusados). */
    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResponse> historico(@PathVariable String id) {
        service.buscarConta(id); // garante 404 para conta inexistente, em vez de lista vazia
        return service.listarPagamentos(id).stream()
                .map(PagamentoResponse::de)
                .toList();
    }
}
