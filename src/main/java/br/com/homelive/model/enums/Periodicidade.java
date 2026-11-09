package br.com.homelive.model.enums;

import java.time.LocalDate;
import java.time.YearMonth;
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

    /**
     * Próxima data respeitando o dia original da escala (o "dia âncora"). Só a mensal precisa dele: sem isso,
     * 31/jan → 28/fev → 28/mar, porque cada passo parte da data anterior e o 31 se perde no mês curto.
     * Com a âncora: 31/jan → 28/fev → 31/mar → 30/abr → 31/mai. As demais periodicidades ignoram a âncora.
     */
    public LocalDate proxima(LocalDate base, int diaAncora) {
        if (this != MENSAL) {
            return proxima(base);
        }
        YearMonth proximoMes = YearMonth.from(base).plusMonths(1);
        return proximoMes.atDay(Math.min(diaAncora, proximoMes.lengthOfMonth()));
    }
}
