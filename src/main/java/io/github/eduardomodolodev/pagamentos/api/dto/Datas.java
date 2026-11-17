package io.github.eduardomodolodev.pagamentos.api.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Padroniza as datas expostas pela API: precisão de segundos (ex.: 2026-11-15T16:30:00). */
final class Datas {

    private Datas() {
    }

    static LocalDateTime emSegundos(LocalDateTime momento) {
        return momento == null ? null : momento.truncatedTo(ChronoUnit.SECONDS);
    }
}
