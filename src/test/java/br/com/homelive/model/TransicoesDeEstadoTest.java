package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Regras de transição de estado vivem dentro das entidades")
class TransicoesDeEstadoTest {

    private Morador morador;
    private Sindico sindico;

    @BeforeEach
    void setUp() {
        morador = new Morador("Lucas", "lucas@test.com", "hash", "11999990001", "101", 30);
        sindico = new Sindico("Helena", "helena@test.com", "hash", "11999990000", "Governança");
    }

    private Despesa novaDespesa() {
        return new Despesa("Internet", "Link", new BigDecimal("100.00"), LocalDate.now().plusDays(10),
                CategoriaDespesa.INTERNET_QUANTICA, TipoDespesa.FIXA, TipoRateio.IGUALITARIO, morador);
    }

    // ---------- Despesa ----------

    @Test
    @DisplayName("Despesa segue PENDENTE -> APROVADA -> RATEADA")
    void despesaCicloFeliz() {
        Despesa d = novaDespesa();
        assertEquals(StatusDespesa.PENDENTE_APROVACAO, d.getStatus());

        d.aprovar(sindico);
        assertEquals(StatusDespesa.APROVADA, d.getStatus());
        assertSame(sindico, d.getAprovador());

        d.marcarComoRateada(List.of(new RateioItem(d, morador, new BigDecimal("100.00"))));
        assertEquals(StatusDespesa.RATEADA, d.getStatus());
        assertEquals(1, d.getItensRateio().size());
    }

    @Test
    @DisplayName("Despesa já avaliada não pode ser aprovada nem rejeitada de novo")
    void despesaNaoPodeSerAvaliadaDuasVezes() {
        Despesa d = novaDespesa();
        d.rejeitar(sindico, "Valor abusivo");

        assertEquals(StatusDespesa.REJEITADA, d.getStatus());
        assertThrows(RegraNegocioException.class, () -> d.aprovar(sindico));
        assertThrows(RegraNegocioException.class, () -> d.rejeitar(sindico, "outra vez"));
    }

    @Test
    @DisplayName("Despesa só pode ser avaliada por um síndico")
    void despesaExigeSindico() {
        assertThrows(RegraNegocioException.class, () -> novaDespesa().aprovar(null));
    }

    @Test
    @DisplayName("Despesa não aprovada não pode ser rateada")
    void despesaPendenteNaoPodeSerRateada() {
        Despesa d = novaDespesa();
        List<RateioItem> itens = List.of(new RateioItem(d, morador, new BigDecimal("100.00")));

        assertThrows(RegraNegocioException.class, () -> d.marcarComoRateada(itens));
        assertEquals(StatusDespesa.PENDENTE_APROVACAO, d.getStatus());
    }

    @Test
    @DisplayName("Lista de itens do rateio é somente leitura para quem está fora da entidade")
    void itensRateioSaoImutaveisPorFora() {
        Despesa d = novaDespesa();
        assertThrows(UnsupportedOperationException.class,
                () -> d.getItensRateio().add(new RateioItem(d, morador, BigDecimal.ONE)));
    }

    // ---------- RateioItem ----------

    @Test
    @DisplayName("Item de rateio só pode ser quitado uma vez")
    void rateioItemQuitaUmaVez() {
        RateioItem item = new RateioItem(novaDespesa(), morador, new BigDecimal("50.00"));

        item.quitar();
        assertEquals(StatusPagamento.PAGO, item.getStatusPagamento());
        assertNotNull(item.getDataPagamento());
        assertThrows(RegraNegocioException.class, item::quitar);
    }

    // ---------- TarefaDomestica ----------

    private TarefaDomestica novaTarefa() {
        return new TarefaDomestica("Lixo", "Tirar o lixo", LocalDate.now().plusDays(1), 25, 15, morador, sindico);
    }

    @Test
    @DisplayName("Concluir tarefa bonifica o responsável e só pode acontecer uma vez")
    void tarefaConcluiUmaVezEPontua() {
        TarefaDomestica t = novaTarefa();

        t.concluir();
        assertEquals(StatusTarefa.CONCLUIDA, t.getStatus());
        assertEquals(125, morador.getPontosConvivencia());
        assertThrows(RegraNegocioException.class, t::concluir);
        assertEquals(125, morador.getPontosConvivencia(), "não pode pontuar duas vezes");
    }

    @Test
    @DisplayName("Concluir tarefa já marcada como atrasada rende só 50% da recompensa")
    void tarefaAtrasadaRendeMetade() {
        TarefaDomestica t = new TarefaDomestica("Lixo", "x", LocalDate.now().minusDays(1), 25, 15, morador, sindico);
        t.penalizarAtraso();              // 100 - 15 = 85
        assertEquals(StatusTarefa.ATRASADA, t.getStatus());

        t.concluir();                     // + 25/2 = 12

        assertEquals(StatusTarefa.CONCLUIDA, t.getStatus());
        assertEquals(97, morador.getPontosConvivencia());
    }

    @Test
    @DisplayName("Concluir depois do prazo rende 50% mesmo se o atraso ainda não foi processado")
    void tarefaForaDoPrazoNaoProcessadaRendeMetade() {
        TarefaDomestica t = new TarefaDomestica("Lixo", "x", LocalDate.now().minusDays(1), 25, 15, morador, sindico);
        assertEquals(StatusTarefa.PENDENTE, t.getStatus());

        t.concluir();

        assertEquals(112, morador.getPontosConvivencia());
    }

    @Test
    @DisplayName("Tarefa atrasada penaliza o responsável uma única vez")
    void tarefaAtrasadaPenalizaUmaVez() {
        TarefaDomestica t = new TarefaDomestica("Lixo", "x", LocalDate.now().minusDays(1), 25, 15, morador, sindico);

        t.penalizarAtraso();
        t.penalizarAtraso();

        assertEquals(StatusTarefa.ATRASADA, t.getStatus());
        assertEquals(85, morador.getPontosConvivencia());
    }

    // ---------- Morador ----------

    @Test
    @DisplayName("Pontos de convivência nunca ficam negativos e quantidades inválidas são recusadas")
    void moradorPontos() {
        morador.deduzirPontos(1000);
        assertEquals(0, morador.getPontosConvivencia());

        assertThrows(RegraNegocioException.class, () -> morador.adicionarPontos(-5));
        assertThrows(RegraNegocioException.class, () -> morador.deduzirPontos(-5));
    }

    @Test
    @DisplayName("Estadia só aceita valores válidos")
    void moradorEstadia() {
        morador.atualizarEstadia(12);
        assertEquals(12, morador.getDiasEstadia());

        assertThrows(RegraNegocioException.class, () -> morador.atualizarEstadia(-1));
        assertThrows(RegraNegocioException.class, () -> morador.atualizarEstadia(null));
        assertEquals(12, morador.getDiasEstadia(), "valor inválido não pode alterar o estado");
    }

    // ---------- AgendamentoEspaco ----------

    private final LocalDateTime amanha = LocalDateTime.now().plusDays(1);

    @Test
    @DisplayName("Reserva inválida nem chega a existir: o construtor recusa")
    void agendamentoConstrutorValida() {
        EspacoComum espaco = new EspacoComum("Lavanderia", "IoT", 2, "x");

        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(espaco, morador, amanha, amanha.minusHours(1), "fim antes do início"));
        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(espaco, morador, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "passado"));
        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(espaco, morador, null, amanha, "sem início"));
        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(null, morador, amanha, amanha.plusHours(1), "sem espaço"));
    }

    @Test
    @DisplayName("Não reserva espaço inativo nem para morador inativo")
    void agendamentoExigeAtivos() {
        EspacoComum espaco = new EspacoComum("Lavanderia", "IoT", 2, "x");

        espaco.desativar();
        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(espaco, morador, amanha, amanha.plusHours(1), "espaço inativo"));

        espaco.ativar();
        morador.desativar();
        assertThrows(RegraNegocioException.class,
                () -> new AgendamentoEspaco(espaco, morador, amanha, amanha.plusHours(1), "morador inativo"));
    }

    @Test
    @DisplayName("Reserva só pode ser cancelada uma vez")
    void agendamentoCancelaUmaVez() {
        EspacoComum espaco = new EspacoComum("Lavanderia", "IoT", 2, "x");
        AgendamentoEspaco a = new AgendamentoEspaco(espaco, morador, amanha, amanha.plusHours(1), "ok");

        a.cancelar();
        assertEquals(StatusAgendamento.CANCELADO, a.getStatus());
        assertThrows(RegraNegocioException.class, a::cancelar);
    }

    // ---------- EspacoComum ----------

    @Test
    @DisplayName("Espaço alterna entre ativo e inativo")
    void espacoAlternaStatus() {
        EspacoComum espaco = new EspacoComum("Gourmet", "Rooftop", 8, "x");

        espaco.alternarStatus();
        assertFalse(espaco.isAtivo());
        espaco.alternarStatus();
        assertTrue(espaco.isAtivo());
    }
}
