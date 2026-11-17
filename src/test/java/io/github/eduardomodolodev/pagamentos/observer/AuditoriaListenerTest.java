package io.github.eduardomodolodev.pagamentos.observer;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.StatusPagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaListenerTest {

    @Test
    void deveGuardarTodosOsEventosEmMemoria() {
        AuditoriaListener auditoria = new AuditoriaListener();
        Pagamento aprovado = Resultados.pix("200.00");
        Pagamento recusado = Resultados.pix("50.00");

        auditoria.aoFinalizar(aprovado, Resultados.finalizar(aprovado, StatusPagamento.APROVADO, null));
        auditoria.aoFinalizar(recusado,
                Resultados.finalizar(recusado, StatusPagamento.RECUSADO, CodigoRecusa.FRAUDE_VELOCIDADE));

        List<AuditoriaListener.Evento> eventos = auditoria.getEventos();
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0).pagamentoId()).isEqualTo(aprovado.getId());
        assertThat(eventos.get(0).tipo()).isEqualTo(TipoPagamento.PIX);
        assertThat(eventos.get(0).status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(eventos.get(1).codigo()).isEqualTo(CodigoRecusa.FRAUDE_VELOCIDADE);
        assertThat(eventos.get(1).processadoEm()).isEqualTo(Resultados.PROCESSADO_EM);
    }

    @Test
    void listaDeEventosDevolvidaNaoPodeSerAlterada() {
        AuditoriaListener auditoria = new AuditoriaListener();
        Pagamento pagamento = Resultados.pix("10.00");
        auditoria.aoFinalizar(pagamento, Resultados.finalizar(pagamento, StatusPagamento.APROVADO, null));

        List<AuditoriaListener.Evento> eventos = auditoria.getEventos();

        org.assertj.core.api.Assertions.assertThatThrownBy(eventos::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveAcrescentarLinhasNoArquivoQuandoConfigurado(@TempDir Path pasta) throws IOException {
        Path arquivo = pasta.resolve("auditoria.log");
        AuditoriaListener auditoria = new AuditoriaListener(arquivo);
        Pagamento primeiro = Resultados.pix("200.00");
        Pagamento segundo = Resultados.pix("50.00");

        auditoria.aoFinalizar(primeiro, Resultados.finalizar(primeiro, StatusPagamento.APROVADO, null));
        auditoria.aoFinalizar(segundo,
                Resultados.finalizar(segundo, StatusPagamento.RECUSADO, CodigoRecusa.SALDO_INSUFICIENTE));

        List<String> linhas = Files.readAllLines(arquivo);
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0)).isEqualTo(
                "2026-11-17T10:30;" + primeiro.getId() + ";CONTA-001;PIX;200.00;200.00;APROVADO;");
        assertThat(linhas.get(1)).endsWith(";RECUSADO;SALDO_INSUFICIENTE");
    }
}
