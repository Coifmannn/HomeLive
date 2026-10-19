package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Leitura de consumo")
class LeituraConsumoTest {

    private Morador lucas;

    @BeforeEach
    void setUp() {
        lucas = new Morador("Lucas", "lucas@test.com", "hash", "11999990001", "101", 30);
    }

    @Test
    @DisplayName("Guarda o mês de referência sempre como o dia 1")
    void normalizaCompetencia() {
        LeituraConsumo l = new LeituraConsumo(lucas, YearMonth.of(2026, 3), new BigDecimal("120.50"));

        assertEquals(LocalDate.of(2026, 3, 1), l.getCompetencia());
        assertEquals(YearMonth.of(2026, 3), l.getMes());
        assertEquals(new BigDecimal("120.50"), l.getKwh());
        assertNotNull(l.getRegistradaEm());
    }

    @Test
    @DisplayName("Aceita o mês corrente e consumo zero; recusa mês futuro, consumo negativo e dados faltando")
    void validacoes() {
        assertDoesNotThrow(() -> new LeituraConsumo(lucas, YearMonth.now(), BigDecimal.ZERO));

        assertThrows(RegraNegocioException.class,
                () -> new LeituraConsumo(lucas, YearMonth.now().plusMonths(1), BigDecimal.TEN));
        assertThrows(RegraNegocioException.class,
                () -> new LeituraConsumo(lucas, YearMonth.now(), new BigDecimal("-0.01")));
        assertThrows(RegraNegocioException.class, () -> new LeituraConsumo(lucas, YearMonth.now(), null));
        assertThrows(RegraNegocioException.class, () -> new LeituraConsumo(lucas, null, BigDecimal.TEN));
        assertThrows(RegraNegocioException.class, () -> new LeituraConsumo(null, YearMonth.now(), BigDecimal.TEN));
    }

    @Test
    @DisplayName("Corrigir troca o valor, mas recusa valor inválido sem alterar o estado")
    void corrigir() {
        LeituraConsumo l = new LeituraConsumo(lucas, YearMonth.now(), new BigDecimal("100.00"));

        l.corrigir(new BigDecimal("130.00"));
        assertEquals(new BigDecimal("130.00"), l.getKwh());

        assertThrows(RegraNegocioException.class, () -> l.corrigir(new BigDecimal("-1")));
        assertEquals(new BigDecimal("130.00"), l.getKwh());
    }
}
