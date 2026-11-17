package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.service.ContaNaoEncontradaException;
import io.github.eduardomodolodev.pagamentos.service.PagamentoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

/**
 * Tratamento global de erros da API, no formato padrão
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">Problem Details</a>.
 *
 * <ul>
 *     <li>400: JSON inválido ou campos obrigatórios ausentes;</li>
 *     <li>404: conta ou pagamento inexistente;</li>
 *     <li>409: operação incompatível com o estado do pagamento (ex.: confirmar
 *         um boleto que não está pendente).</li>
 * </ul>
 *
 * <p>Recusas de negócio <b>não</b> passam por aqui: elas voltam como
 * pagamento {@code RECUSADO} com 201.
 */
@RestControllerAdvice
public class TratamentoErrosApi {

    @ExceptionHandler(ContaNaoEncontradaException.class)
    ProblemDetail contaNaoEncontrada(ContaNaoEncontradaException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
        problema.setTitle("Conta não encontrada");
        problema.setProperty("contaId", erro.getContaId());
        return problema;
    }

    @ExceptionHandler(PagamentoNaoEncontradoException.class)
    ProblemDetail pagamentoNaoEncontrado(PagamentoNaoEncontradoException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
        problema.setTitle("Pagamento não encontrado");
        problema.setProperty("pagamentoId", erro.getPagamentoId());
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail requisicaoInvalida(MethodArgumentNotValidException erro) {
        Map<String, String> campos = new TreeMap<>();
        for (FieldError campo : erro.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais campos são inválidos");
        problema.setTitle("Requisição inválida");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido(HttpMessageNotReadableException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O corpo da requisição não é um JSON válido ou tem um valor em formato incorreto "
                        + "(ex.: tipo de pagamento desconhecido, data fora do padrão AAAA-MM-DD)");
        problema.setTitle("JSON inválido");
        return problema;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflitoDeEstado(IllegalStateException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
        problema.setTitle("Operação não permitida no estado atual");
        return problema;
    }
}
