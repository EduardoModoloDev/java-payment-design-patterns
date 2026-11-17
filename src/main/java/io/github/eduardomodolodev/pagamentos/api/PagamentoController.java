package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoRequest;
import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoResponse;
import io.github.eduardomodolodev.pagamentos.api.dto.ResultadoPagamentoResponse;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.service.PagamentoNaoEncontradoException;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Clock;

/**
 * Endpoints de pagamento. O controller só traduz HTTP ↔ DTO; toda a regra
 * fica no {@link PagamentoService} (Facade).
 */
@RestController
@RequestMapping("/api/pagamentos")
@Tag(name = "Pagamentos", description = "Processamento, consulta e confirmação de pagamentos")
public class PagamentoController {

    private static final String EXEMPLO_CARTAO = """
            {
              "contaOrigemId": "CONTA-001",
              "tipo": "CARTAO_CREDITO",
              "valor": 1200.00,
              "destinatario": "Loja do Zé",
              "parcelas": 6
            }""";

    private final PagamentoService service;
    private final Clock clock;

    public PagamentoController(PagamentoService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /**
     * Cria e processa um pagamento. Responde 201 inclusive quando o pagamento é
     * recusado por regra de negócio: {@code status} e {@code codigo} no corpo
     * dizem o que houve.
     */
    @PostMapping
    @Operation(summary = "Cria e processa um pagamento",
            description = "Passa pela cadeia de validações, aplica taxas/juros/cashback e notifica os listeners. "
                    + "Recusas de negócio também respondem 201, com status RECUSADO e o motivo em `codigo`.")
    @ApiResponse(responseCode = "201", description = "Pagamento processado (aprovado, pendente ou recusado)")
    @ApiResponse(responseCode = "400", description = "JSON inválido ou campos obrigatórios ausentes",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Conta de origem inexistente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ResultadoPagamentoResponse> processar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(name = "Cartão em 6x", value = EXEMPLO_CARTAO)))
            @Valid @RequestBody PagamentoRequest requisicao) {
        ResultadoPagamento resultado = service.processar(requisicao.paraPagamento(clock));
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resultado.pagamentoId())
                .toUri();
        return ResponseEntity.created(local).body(ResultadoPagamentoResponse.de(resultado));
    }

    /** Consulta um pagamento (404 se não existir). */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pagamento")
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado")
    @ApiResponse(responseCode = "404", description = "Pagamento inexistente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PagamentoResponse buscar(@PathVariable String id) {
        return service.buscarPagamento(id)
                .map(PagamentoResponse::de)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(id));
    }

    /** Confirma (paga) um boleto pendente. */
    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirma um boleto pendente",
            description = "Simula o pagamento do boleto: debita valor + taxa e aprova, "
                    + "ou recusa com SALDO_INSUFICIENTE.")
    @ApiResponse(responseCode = "200", description = "Boleto confirmado (aprovado ou recusado)")
    @ApiResponse(responseCode = "404", description = "Pagamento inexistente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O pagamento não é um boleto pendente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResultadoPagamentoResponse confirmarBoleto(@PathVariable String id) {
        return ResultadoPagamentoResponse.de(service.confirmarBoleto(id));
    }
}
