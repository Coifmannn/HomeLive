package br.com.homelive.strategy;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Padrão Strategy para Rateio de Custos")
class CalculoRateioStrategyTest {

    private Morador m1;
    private Morador m2;
    private Morador m3;
    private List<Morador> moradores;

    @BeforeEach
    void setUp() {
        m1 = new Morador("Lucas", "lucas@test.com", "11999990001", "101", 30, 100.0);
        m1.setId(1L);

        m2 = new Morador("Sofia", "sofia@test.com", "11999990002", "102", 30, 200.0);
        m2.setId(2L);

        m3 = new Morador("Mateus", "mateus@test.com", "11999990003", "103", 20, 100.0);
        m3.setId(3L);

        moradores = List.of(m1, m2, m3);
    }

    @Test
    @DisplayName("Rateio Igualitário deve dividir o valor em partes iguais e bater a soma exata")
    void testRateioIgualitario() {
        Despesa despesa = new Despesa(
                "Internet Quântica",
                "Link",
                new BigDecimal("100.00"),
                LocalDate.now().plusDays(10),
                CategoriaDespesa.INTERNET_QUANTICA,
                TipoDespesa.FIXA,
                TipoRateio.IGUALITARIO,
                null
        );

        RateioIgualitarioStrategy strategy = new RateioIgualitarioStrategy();
        List<RateioItem> itens = strategy.calcular(despesa, moradores);

        assertEquals(3, itens.size());

        // Soma deve ser exatamente 100.00 (33.33 + 33.33 + 33.34)
        BigDecimal soma = itens.stream()
                .map(RateioItem::getValorCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("100.00"), soma);
    }

    @Test
    @DisplayName("Rateio por Consumo deve calcular valores proporcionais ao consumo em kWh")
    void testRateioPorConsumo() {
        // Consumos: m1=100 (25%), m2=200 (50%), m3=100 (25%) -> Total = 400 kWh
        Despesa despesa = new Despesa(
                "Energia Solar",
                "Fatura Solar",
                new BigDecimal("400.00"),
                LocalDate.now().plusDays(10),
                CategoriaDespesa.ENERGIA_SOLAR,
                TipoDespesa.VARIAVEL,
                TipoRateio.POR_CONSUMO,
                null
        );

        RateioPorConsumoStrategy strategy = new RateioPorConsumoStrategy();
        List<RateioItem> itens = strategy.calcular(despesa, moradores);

        assertEquals(new BigDecimal("100.00"), itens.get(0).getValorCalculado());
        assertEquals(new BigDecimal("200.00"), itens.get(1).getValorCalculado());
        assertEquals(new BigDecimal("100.00"), itens.get(2).getValorCalculado());

        BigDecimal soma = itens.stream()
                .map(RateioItem::getValorCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("400.00"), soma);
    }

    @Test
    @DisplayName("Rateio por Estadia deve calcular valores proporcionais aos dias de estadia")
    void testRateioPorEstadia() {
        // Estadias: m1=30, m2=30, m3=20 -> Total = 80 dias
        Despesa despesa = new Despesa(
                "Manutenção Robôs",
                "Revisão",
                new BigDecimal("800.00"),
                LocalDate.now().plusDays(10),
                CategoriaDespesa.MANUTENCAO_ROBOS,
                TipoDespesa.VARIAVEL,
                TipoRateio.POR_ESTADIA,
                null
        );

        RateioPorEstadiaStrategy strategy = new RateioPorEstadiaStrategy();
        List<RateioItem> itens = strategy.calcular(despesa, moradores);

        assertEquals(new BigDecimal("300.00"), itens.get(0).getValorCalculado());
        assertEquals(new BigDecimal("300.00"), itens.get(1).getValorCalculado());
        assertEquals(new BigDecimal("200.00"), itens.get(2).getValorCalculado());

        BigDecimal soma = itens.stream()
                .map(RateioItem::getValorCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("800.00"), soma);
    }
}
