package br.com.homelive.model.enums;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

/** Cada periodicidade sabe calcular a data seguinte a partir de uma data base. */
public enum Periodicidade {
    DIARIA(d -> d.plusDays(1)),
    SEMANAL(d -> d.plusWeeks(1)),
    QUINZENAL(d -> d.plusWeeks(2)),
    MENSAL(d -> d.plusMonths(1));

    private final UnaryOperator<LocalDate> avanco;

    Periodicidade(UnaryOperator<LocalDate> avanco) {
        this.avanco = avanco;
    }

    public LocalDate proxima(LocalDate base) {
        return avanco.apply(base);
    }
}
