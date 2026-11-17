package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.api.dto.ContaResponse;
import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoResponse;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de contas e do histórico de pagamentos de cada uma. */
@RestController
@RequestMapping("/api/contas")
@Tag(name = "Contas", description = "Saldo, limites e histórico de pagamentos")
@ApiResponse(responseCode = "404", description = "Conta inexistente",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
public class ContaController {

    private final PagamentoService service;

    public ContaController(PagamentoService service) {
        this.service = service;
    }

    /** Saldo, limites e dados da conta. */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta saldo, limites e dados da conta")
    public ContaResponse buscar(@PathVariable String id) {
        return ContaResponse.de(service.buscarConta(id));
    }

    /** Histórico de pagamentos da conta, na ordem em que foram feitos (inclui recusados). */
    @GetMapping("/{id}/pagamentos")
    @Operation(summary = "Histórico de pagamentos da conta",
            description = "Na ordem em que foram feitos, incluindo os recusados.")
    public List<PagamentoResponse> historico(@PathVariable String id) {
        service.buscarConta(id); // garante 404 para conta inexistente, em vez de lista vazia
        return service.listarPagamentos(id).stream()
                .map(PagamentoResponse::de)
                .toList();
    }
}
