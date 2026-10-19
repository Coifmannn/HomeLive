package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.model.enums.TipoRodizio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tarefa recorrente")
class TarefaRecorrenteTest {

    private final LocalDate hoje = LocalDate.now();
    private Morador lucas;

    @BeforeEach
    void setUp() {
        lucas = new Morador("Lucas", "lucas@test.com", "hash", "11999990001", "101", 30, 100.0);
    }

    private TarefaRecorrente semanal() {
        return new TarefaRecorrente("Lixo", "Levar ao térreo", Periodicidade.SEMANAL, 2, 20, 10,
                TipoRodizio.SEQUENCIAL, hoje);
    }

    @Test
    @DisplayName("Periodicidade calcula a data seguinte")
    void periodicidadeAvanca() {
        LocalDate base = LocalDate.of(2030, 1, 31);

        assertEquals(LocalDate.of(2030, 2, 1), Periodicidade.DIARIA.proxima(base));
        assertEquals(LocalDate.of(2030, 2, 7), Periodicidade.SEMANAL.proxima(base));
        assertEquals(LocalDate.of(2030, 2, 14), Periodicidade.QUINZENAL.proxima(base));
        assertEquals(LocalDate.of(2030, 2, 28), Periodicidade.MENSAL.proxima(base));
    }

    @Test
    @DisplayName("Construtor recusa dados inválidos")
    void construtorValida() {
        assertThrows(RegraNegocioException.class, () ->
                new TarefaRecorrente(" ", "x", Periodicidade.SEMANAL, 2, null, null, TipoRodizio.SEQUENCIAL, hoje));
        assertThrows(RegraNegocioException.class, () ->
                new TarefaRecorrente("Lixo", "x", null, 2, null, null, TipoRodizio.SEQUENCIAL, hoje));
        assertThrows(RegraNegocioException.class, () ->
                new TarefaRecorrente("Lixo", "x", Periodicidade.SEMANAL, 2, null, null, null, hoje));
        assertThrows(RegraNegocioException.class, () ->
                new TarefaRecorrente("Lixo", "x", Periodicidade.SEMANAL, 0, null, null, TipoRodizio.SEQUENCIAL, hoje));
        assertThrows(RegraNegocioException.class, () ->
                new TarefaRecorrente("Lixo", "x", Periodicidade.SEMANAL, 2, null, null, TipoRodizio.SEQUENCIAL, hoje.minusDays(1)));
    }

    @Test
    @DisplayName("Sem data de início, a primeira geração é hoje e os pontos têm valor padrão")
    void padroes() {
        TarefaRecorrente t = new TarefaRecorrente("Lixo", null, Periodicidade.DIARIA, 1, null, null, TipoRodizio.MENOR_CARGA, null);

        assertEquals(hoje, t.getProximaGeracao());
        assertTrue(t.devidaEm(hoje));
        assertEquals(25, t.getPontosRecompensa());
        assertEquals(15, t.getPontosPenalidade());
    }

    @Test
    @DisplayName("Gerar cria a tarefa do morador, com prazo e pontos do modelo, e avança a próxima data")
    void geraEAvanca() {
        TarefaRecorrente modelo = semanal();

        TarefaDomestica tarefa = modelo.gerarTarefa(lucas, hoje);

        assertEquals("Lixo", tarefa.getTitulo());
        assertSame(lucas, tarefa.getResponsavel());
        assertNull(tarefa.getAtribuidaPor(), "geradas pelo sistema não têm síndico atribuidor");
        assertEquals(hoje.plusDays(2), tarefa.getDataPrazo());
        assertEquals(20, tarefa.getPontosRecompensa());
        assertEquals(10, tarefa.getPontosPenalidade());
        assertEquals(StatusTarefa.PENDENTE, tarefa.getStatus());

        assertEquals(hoje.plusWeeks(1), modelo.getProximaGeracao());
        assertSame(lucas, modelo.getUltimoResponsavel());
        assertFalse(modelo.devidaEm(hoje));
    }

    @Test
    @DisplayName("Não gera duas vezes no mesmo período")
    void naoGeraDuasVezes() {
        TarefaRecorrente modelo = semanal();
        modelo.gerarTarefa(lucas, hoje);

        assertThrows(RegraNegocioException.class, () -> modelo.gerarTarefa(lucas, hoje));
    }

    @Test
    @DisplayName("Depois de ficar parado, gera uma só tarefa e salta para a próxima data futura")
    void recuperaAtraso() {
        TarefaRecorrente modelo = semanal();
        ReflectionTestUtils.setField(modelo, "proximaGeracao", hoje.minusDays(30));

        modelo.gerarTarefa(lucas, hoje);

        assertTrue(modelo.getProximaGeracao().isAfter(hoje));
        assertFalse(modelo.getProximaGeracao().isAfter(hoje.plusWeeks(1)));
    }

    @Test
    @DisplayName("Modelo inativo ou ainda não devido não gera; sem responsável também não")
    void naoGeraQuandoNaoDevida() {
        TarefaRecorrente futuro = new TarefaRecorrente("Lixo", "x", Periodicidade.SEMANAL, 2, null, null,
                TipoRodizio.SEQUENCIAL, hoje.plusDays(3));
        assertFalse(futuro.devidaEm(hoje));
        assertThrows(RegraNegocioException.class, () -> futuro.gerarTarefa(lucas, hoje));

        TarefaRecorrente modelo = semanal();
        modelo.desativar();
        assertFalse(modelo.devidaEm(hoje));
        assertThrows(RegraNegocioException.class, () -> modelo.gerarTarefa(lucas, hoje));

        modelo.ativar();
        assertTrue(modelo.devidaEm(hoje));
        assertThrows(RegraNegocioException.class, () -> modelo.gerarTarefa(null, hoje));
    }
}
