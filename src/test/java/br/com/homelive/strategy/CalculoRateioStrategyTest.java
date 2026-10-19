package br.com.homelive.strategy;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Padrão Strategy para Rateio de Custos")
class CalculoRateioStrategyTest {

    private Morador m1;
    private Morador m2;
    private Morador m3;
    private List<Morador> moradores;

    /** Leituras do mês da despesa: m1=100 kWh (25%), m2=200 (50%), m3=100 (25%). */
    private ContextoRateio leituras;

    @BeforeEach
    void setUp() {
        m1 = new Morador("Lucas", "lucas@test.com", "hash", "11999990001", "101", 30);
        m2 = new Morador("Sofia", "sofia@test.com", "hash", "11999990002", "102", 30);
        m3 = new Morador("Mateus", "mateus@test.com", "hash", "11999990003", "103", 20);
        ReflectionTestUtils.setField(m1, "id", 1L);
        ReflectionTestUtils.setField(m2, "id", 2L);
        ReflectionTestUtils.setField(m3, "id", 3L);

        moradores = List.of(m1, m2, m3);
        leituras = new ContextoRateio(Map.of(
                1L, new BigDecimal("100.00"),
                2L, new BigDecimal("200.00"),
                3L, new BigDecimal("100.00")));
    }

    private Despesa despesa(String valor, TipoRateio tipo) {
        return new Despesa("Conta", "teste", new BigDecimal(valor), LocalDate.now().plusDays(10),
                CategoriaDespesa.ENERGIA_SOLAR, TipoDespesa.VARIAVEL, tipo, null);
    }

    private BigDecimal soma(List<RateioItem> itens) {
        return itens.stream().map(RateioItem::getValorCalculado).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Rateio Igualitário deve dividir o valor em partes iguais e bater a soma exata")
    void testRateioIgualitario() {
        List<RateioItem> itens = new RateioIgualitarioStrategy()
                .calcular(despesa("100.00", TipoRateio.IGUALITARIO), moradores, ContextoRateio.vazio());

        assertEquals(3, itens.size());
        // 33.33 + 33.33 + 33.34
        assertEquals(new BigDecimal("100.00"), soma(itens));
    }

    @Test
    @DisplayName("Rateio por Consumo deve calcular valores proporcionais às leituras em kWh")
    void testRateioPorConsumo() {
        List<RateioItem> itens = new RateioPorConsumoStrategy()
                .calcular(despesa("400.00", TipoRateio.POR_CONSUMO), moradores, leituras);

        assertEquals(new BigDecimal("100.00"), itens.get(0).getValorCalculado());
        assertEquals(new BigDecimal("200.00"), itens.get(1).getValorCalculado());
        assertEquals(new BigDecimal("100.00"), itens.get(2).getValorCalculado());
        assertEquals(new BigDecimal("400.00"), soma(itens));
    }

    @Test
    @DisplayName("Rateio por Estadia deve calcular valores proporcionais aos dias de estadia")
    void testRateioPorEstadia() {
        // Estadias: m1=30, m2=30, m3=20 -> Total = 80 dias
        List<RateioItem> itens = new RateioPorEstadiaStrategy()
                .calcular(despesa("800.00", TipoRateio.POR_ESTADIA), moradores, ContextoRateio.vazio());

        assertEquals(new BigDecimal("300.00"), itens.get(0).getValorCalculado());
        assertEquals(new BigDecimal("300.00"), itens.get(1).getValorCalculado());
        assertEquals(new BigDecimal("200.00"), itens.get(2).getValorCalculado());
        assertEquals(new BigDecimal("800.00"), soma(itens));
    }

    @Test
    @DisplayName("A soma dos itens fecha exatamente no valor total mesmo com centavos quebrados")
    void testSomaExataComValorQuebrado() {
        Despesa despesa = despesa("1000000.01", TipoRateio.POR_CONSUMO);

        for (CalculoRateioStrategy strategy : List.of(
                new RateioIgualitarioStrategy(), new RateioPorConsumoStrategy(), new RateioPorEstadiaStrategy())) {
            assertEquals(new BigDecimal("1000000.01"), soma(strategy.calcular(despesa, moradores, leituras)),
                    strategy.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("Rateio sem moradores deve ser rejeitado")
    void testRateioSemMoradores() {
        assertThrows(RegraNegocioException.class, () -> new RateioIgualitarioStrategy()
                .calcular(despesa("10.00", TipoRateio.IGUALITARIO), List.of(), ContextoRateio.vazio()));
    }

    @Test
    @DisplayName("Rateio por consumo com leituras todas zeradas deve ser rejeitado")
    void testRateioPorConsumoZerado() {
        ContextoRateio zerado = new ContextoRateio(Map.of(
                1L, BigDecimal.ZERO, 2L, BigDecimal.ZERO, 3L, BigDecimal.ZERO));

        assertThrows(RegraNegocioException.class, () -> new RateioPorConsumoStrategy()
                .calcular(despesa("10.00", TipoRateio.POR_CONSUMO), moradores, zerado));
    }

    @Test
    @DisplayName("Rateio por consumo recusa a despesa e diz quem está sem leitura no mês")
    void testRateioPorConsumoSemLeitura() {
        ContextoRateio incompleto = new ContextoRateio(Map.of(1L, new BigDecimal("100.00")));
        Despesa despesa = despesa("10.00", TipoRateio.POR_CONSUMO);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> new RateioPorConsumoStrategy().calcular(despesa, moradores, incompleto));

        assertTrue(erro.getMessage().contains("Sofia"), erro.getMessage());
        assertTrue(erro.getMessage().contains("Mateus"), erro.getMessage());
        assertFalse(erro.getMessage().contains("Lucas"), erro.getMessage());
        assertTrue(erro.getMessage().contains(String.format("%02d/%d", YearMonth.now().getMonthValue(), YearMonth.now().getYear())),
                "deve citar o mês da conta: " + erro.getMessage());
    }
}
