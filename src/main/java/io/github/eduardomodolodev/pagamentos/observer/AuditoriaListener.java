package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.ResultadoPagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Guarda <b>todo</b> evento de pagamento: sempre numa lista em memória e,
 * opcionalmente, como linhas num arquivo (ex.: {@code auditoria.log}).
 */
public class AuditoriaListener implements PagamentoListener {

    /** Um evento auditado. */
    public record Evento(
            LocalDateTime processadoEm,
            String pagamentoId,
            String contaId,
            TipoPagamento tipo,
            BigDecimal valor,
            BigDecimal total,
            StatusPagamento status,
            String codigo) {

        /** Linha no formato gravado no arquivo (separada por {@code ;}). */
        public String comoLinha() {
            return String.join(";", String.valueOf(processadoEm), pagamentoId, contaId, tipo.name(),
                    valor.toPlainString(), total.toPlainString(), status.name(), codigo == null ? "" : codigo);
        }
    }

    private final List<Evento> eventos = Collections.synchronizedList(new ArrayList<>());
    private final Path arquivo;

    /** Auditoria só em memória. */
    public AuditoriaListener() {
        this(null);
    }

    /** Auditoria em memória e também no arquivo informado (acrescenta linhas). */
    public AuditoriaListener(Path arquivo) {
        this.arquivo = arquivo;
    }

    @Override
    public void aoFinalizar(Pagamento pagamento, ResultadoPagamento resultado) {
        Evento evento = new Evento(resultado.processadoEm(), pagamento.getId(), pagamento.getContaOrigemId(),
                pagamento.getTipo(), resultado.valor(), resultado.total(), resultado.status(), resultado.codigo());
        eventos.add(evento);
        if (arquivo != null) {
            gravar(evento);
        }
    }

    public List<Evento> getEventos() {
        synchronized (eventos) {
            return List.copyOf(eventos);
        }
    }

    private void gravar(Evento evento) {
        try {
            Files.writeString(arquivo, evento.comoLinha() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar auditoria em " + arquivo, e);
        }
    }
}
