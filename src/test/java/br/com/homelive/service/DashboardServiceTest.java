package br.com.homelive.service;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.StatusDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import br.com.homelive.repository.DespesaRepository;
import br.com.homelive.repository.MoradorRepository;
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

    @Autowired
    private MoradorRepository moradorRepository;

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
    @DisplayName("Despesa rejeitada não entra no total do mês")
    void ignoraDespesaRejeitada() {
        BigDecimal antes = dashboardService.obterResumoGeral().getTotalDespesasMes();

        Despesa rejeitada = despesaLancadaEm(LocalDate.now(), "300.00");
        ReflectionTestUtils.setField(rejeitada, "status", StatusDespesa.REJEITADA);
        despesaRepository.saveAndFlush(rejeitada);
        despesaLancadaEm(LocalDate.now(), "20.00"); // pendente: continua contando

        assertEquals(0, antes.add(new BigDecimal("20.00")).compareTo(dashboardService.obterResumoGeral().getTotalDespesasMes()));
    }

    @Test
    @DisplayName("Morador desativado não conta no total de moradores da casa")
    void ignoraMoradorInativo() {
        long antes = dashboardService.obterResumoGeral().getTotalMoradores();

        Morador saiu = moradorRepository.findByAtivoTrue().get(0);
        saiu.desativar();
        moradorRepository.saveAndFlush(saiu);

        assertEquals(antes - 1, dashboardService.obterResumoGeral().getTotalMoradores());
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
