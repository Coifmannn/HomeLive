package br.com.homelive.service;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import br.com.homelive.repository.DespesaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@DisplayName("Dashboard: totais do mês")
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DespesaRepository despesaRepository;

    private Despesa despesaLancadaEm(LocalDate data, String valor) {
        Despesa d = new Despesa("Conta", "teste", new BigDecimal(valor), LocalDate.now().plusDays(5),
                CategoriaDespesa.INTERNET_QUANTICA, TipoDespesa.FIXA, TipoRateio.IGUALITARIO, null);
        ReflectionTestUtils.setField(d, "dataLancamento", data);
        return despesaRepository.saveAndFlush(d);
    }

    @Test
    @DisplayName("Despesa do mesmo mês do ano passado não entra no total do mês")
    void ignoraMesmoMesDeOutroAno() {
        BigDecimal antes = dashboardService.obterResumoGeral().getTotalDespesasMes();

        despesaLancadaEm(LocalDate.now().minusYears(1), "999.00");

        assertEquals(0, antes.compareTo(dashboardService.obterResumoGeral().getTotalDespesasMes()));
    }

    @Test
    @DisplayName("Despesa do mês corrente entra no total, e a do mês anterior não")
    void somenteMesCorrente() {
        BigDecimal antes = dashboardService.obterResumoGeral().getTotalDespesasMes();

        despesaLancadaEm(LocalDate.now(), "10.00");
        despesaLancadaEm(LocalDate.now().withDayOfMonth(1).minusDays(1), "500.00");

        assertEquals(0, antes.add(new BigDecimal("10.00")).compareTo(dashboardService.obterResumoGeral().getTotalDespesasMes()));
    }

    @Test
    @DisplayName("Primeiro e último dia do mês corrente entram no total")
    void bordasDoMes() {
        BigDecimal antes = dashboardService.obterResumoGeral().getTotalDespesasMes();

        LocalDate primeiro = LocalDate.now().withDayOfMonth(1);
        despesaLancadaEm(primeiro, "1.00");
        despesaLancadaEm(primeiro.plusMonths(1).minusDays(1), "2.00");

        assertEquals(0, antes.add(new BigDecimal("3.00")).compareTo(dashboardService.obterResumoGeral().getTotalDespesasMes()));
    }
}
