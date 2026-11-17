package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoRequest;
import io.github.eduardomodolodev.pagamentos.api.dto.PagamentoResponse;
import io.github.eduardomodolodev.pagamentos.api.dto.ResultadoPagamentoResponse;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.service.PagamentoNaoEncontradoException;
import io.github.eduardomodolodev.pagamentos.service.PagamentoService;
import jakarta.validation.Valid;
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
public class PagamentoController {

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
    public ResponseEntity<ResultadoPagamentoResponse> processar(@Valid @RequestBody PagamentoRequest requisicao) {
        ResultadoPagamento resultado = service.processar(requisicao.paraPagamento(clock));
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resultado.pagamentoId())
                .toUri();
        return ResponseEntity.created(local).body(ResultadoPagamentoResponse.de(resultado));
    }

    /** Consulta um pagamento (404 se não existir). */
    @GetMapping("/{id}")
    public PagamentoResponse buscar(@PathVariable String id) {
        return service.buscarPagamento(id)
                .map(PagamentoResponse::de)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(id));
    }

    /** Confirma (paga) um boleto pendente. */
    @PostMapping("/{id}/confirmar")
    public ResultadoPagamentoResponse confirmarBoleto(@PathVariable String id) {
        return ResultadoPagamentoResponse.de(service.confirmarBoleto(id));
    }
}
