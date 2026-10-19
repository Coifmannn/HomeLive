package br.com.homelive.service;

import br.com.homelive.dto.DespesaDTO;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;
import br.com.homelive.model.LeituraConsumo;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import br.com.homelive.repository.LeituraConsumoRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Rateio por consumo usa as leituras do mês da conta")
class RateioPorCompetenciaIntegracaoTest {

    @Autowired DespesaService despesaService;
    @Autowired SindicoRepository sindicoRepository;
    @Autowired MoradorRepository moradorRepository;
    @Autowired LeituraConsumoRepository leituraRepository;

    private Sindico sindico;
    private UsuarioAutenticado ator;

    @BeforeEach
    void setUp() {
        sindico = sindicoRepository.findAll().get(0);
        ator = new UsuarioAutenticado(sindico);
    }

    private Map<String, BigDecimal> ratear(String valor, YearMonth competencia) {
        DespesaDTO.Request req = new DespesaDTO.Request();
        req.setTitulo("Energia " + competencia);
        req.setValorTotal(new BigDecimal(valor));
        req.setDataVencimento(LocalDate.now().plusDays(10));
        req.setCategoria(CategoriaDespesa.ENERGIA_SOLAR);
        req.setTipoDespesa(TipoDespesa.VARIAVEL);
        req.setTipoRateio(TipoRateio.POR_CONSUMO);
        req.setCompetencia(competencia.toString());

        DespesaDTO.Response lancada = despesaService.lancarDespesa(req, ator);
        assertEquals(competencia.toString(), lancada.getCompetencia());

        DespesaDTO.AprovacaoRequest aprovacao = new DespesaDTO.AprovacaoRequest();
        aprovacao.setAprovado(true);
        return despesaService.avaliarDespesa(lancada.getId(), aprovacao, sindico.getId()).getItensRateio().stream()
                .collect(Collectors.toMap(DespesaDTO.RateioItemResponse::getMoradorNome,
                        DespesaDTO.RateioItemResponse::getValorCalculado));
    }

    @Test
    @DisplayName("A mesma conta é dividida de forma diferente conforme o mês de referência")
    void cadaMesUsaSuasLeituras() {
        // Dados de demonstração. Mês anterior: 120/100/70/150 = 440 kWh. Mês atual: 140/95/60/165 = 460 kWh.
        Map<String, BigDecimal> anterior = ratear("440.00", YearMonth.now().minusMonths(1));
        Map<String, BigDecimal> atual = ratear("460.00", YearMonth.now());

        assertEquals(new BigDecimal("120.00"), anterior.get("Lucas Silva"));
        assertEquals(new BigDecimal("100.00"), anterior.get("Sofia Chen"));
        assertEquals(new BigDecimal("70.00"), anterior.get("Mateus Oliveira"));
        assertEquals(new BigDecimal("150.00"), anterior.get("Beatriz Santos"));

        assertEquals(new BigDecimal("140.00"), atual.get("Lucas Silva"));
        assertEquals(new BigDecimal("95.00"), atual.get("Sofia Chen"));
        assertEquals(new BigDecimal("60.00"), atual.get("Mateus Oliveira"));
        assertEquals(new BigDecimal("165.00"), atual.get("Beatriz Santos"));
    }

    @Test
    @DisplayName("Mudar a leitura de outro mês não afeta o rateio deste mês")
    void leituraDeOutroMesNaoInterfere() {
        Morador lucas = moradorRepository.findAll().stream().filter(m -> m.getNome().equals("Lucas Silva")).findFirst().orElseThrow();
        leituraRepository.saveAndFlush(new LeituraConsumo(lucas, YearMonth.now().minusMonths(3), new BigDecimal("9999.00")));

        Map<String, BigDecimal> atual = ratear("460.00", YearMonth.now());

        assertEquals(new BigDecimal("140.00"), atual.get("Lucas Silva"));
    }

    @Test
    @DisplayName("Sem nenhuma leitura no mês da conta, a aprovação é recusada citando todos")
    void semLeituras() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> ratear("100.00", YearMonth.now().minusMonths(2)));

        for (String nome : List.of("Lucas Silva", "Sofia Chen", "Mateus Oliveira", "Beatriz Santos")) {
            assertTrue(erro.getMessage().contains(nome), erro.getMessage());
        }
    }

    @Test
    @DisplayName("Com leitura incompleta, a recusa cita só quem falta")
    void leituraIncompleta() {
        YearMonth mes = YearMonth.now().minusMonths(2);
        List<Morador> moradores = moradorRepository.findByAtivoTrue();
        Morador semLeitura = moradores.get(moradores.size() - 1);
        moradores.stream().filter(m -> !m.equals(semLeitura))
                .forEach(m -> leituraRepository.save(new LeituraConsumo(m, mes, new BigDecimal("50.00"))));
        leituraRepository.flush();

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> ratear("100.00", mes));

        assertTrue(erro.getMessage().contains(semLeitura.getNome()), erro.getMessage());
        assertEquals(1, moradores.stream().filter(m -> erro.getMessage().contains(m.getNome())).count());
    }

    @Test
    @DisplayName("Despesa com competência futura é recusada")
    void competenciaFutura() {
        assertThrows(RegraNegocioException.class, () -> ratear("100.00", YearMonth.now().plusMonths(1)));
    }

    @Test
    @DisplayName("Rateio que não depende de consumo ignora a falta de leituras")
    void igualitarioNaoPrecisaDeLeitura() {
        DespesaDTO.Request req = new DespesaDTO.Request();
        req.setTitulo("Internet");
        req.setValorTotal(new BigDecimal("100.00"));
        req.setDataVencimento(LocalDate.now().plusDays(5));
        req.setCategoria(CategoriaDespesa.INTERNET_QUANTICA);
        req.setTipoDespesa(TipoDespesa.FIXA);
        req.setTipoRateio(TipoRateio.IGUALITARIO);
        req.setCompetencia(YearMonth.now().minusMonths(5).toString());

        Long id = despesaService.lancarDespesa(req, ator).getId();
        DespesaDTO.AprovacaoRequest aprovacao = new DespesaDTO.AprovacaoRequest();
        aprovacao.setAprovado(true);

        assertEquals(4, despesaService.avaliarDespesa(id, aprovacao, sindico.getId()).getItensRateio().size());
    }
}
