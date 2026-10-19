package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.TipoInfracao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Regra de penalidade")
class RegraPenalidadeTest {

    private RegraPenalidade fixa(int pontos) {
        return new RegraPenalidade("Fixa", TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, pontos, null, null, null, null);
    }

    private RegraPenalidade progressiva(int base, int incremento, Integer teto) {
        return new RegraPenalidade("Progressiva", TipoInfracao.TAREFA_ATRASADA, ModoCalculoPenalidade.PROGRESSIVA,
                base, incremento, teto, null, null);
    }

    @Test
    @DisplayName("Modo FIXA desconta sempre os mesmos pontos, não importa a reincidência")
    void modoFixa() {
        RegraPenalidade r = fixa(20);

        assertEquals(20, r.pontosPara(0));
        assertEquals(20, r.pontosPara(1));
        assertEquals(20, r.pontosPara(50));
    }

    @Test
    @DisplayName("Modo PROGRESSIVA soma o incremento a cada reincidência")
    void modoProgressiva() {
        RegraPenalidade r = progressiva(10, 5, null);

        assertEquals(10, r.pontosPara(0));
        assertEquals(15, r.pontosPara(1));
        assertEquals(20, r.pontosPara(2));
        assertEquals(60, r.pontosPara(10));
    }

    @Test
    @DisplayName("Modo PROGRESSIVA respeita o teto")
    void progressivaComTeto() {
        RegraPenalidade r = progressiva(10, 5, 30);

        assertEquals(25, r.pontosPara(3));
        assertEquals(30, r.pontosPara(4));
        assertEquals(30, r.pontosPara(1000));
    }

    @Test
    @DisplayName("Cancelamento é tardio quando falta menos que o limite de horas, ou já passou do início")
    void cancelamentoTardio() {
        RegraPenalidade r = new RegraPenalidade("Tardio", TipoInfracao.CANCELAMENTO_TARDIO,
                ModoCalculoPenalidade.FIXA, 10, null, null, null, 2);
        LocalDateTime inicio = LocalDateTime.of(2030, 5, 10, 14, 0);

        assertFalse(r.cancelamentoEhTardio(inicio, inicio.minusHours(3)), "3h antes: dentro do prazo");
        assertFalse(r.cancelamentoEhTardio(inicio, inicio.minusHours(2)), "exatamente 2h antes: ainda vale");
        assertTrue(r.cancelamentoEhTardio(inicio, inicio.minusHours(2).plusMinutes(1)), "1h59 antes: tardio");
        assertTrue(r.cancelamentoEhTardio(inicio, inicio.minusMinutes(5)));
        assertTrue(r.cancelamentoEhTardio(inicio, inicio.plusMinutes(5)), "depois do início também é tardio");
    }

    @Test
    @DisplayName("Valores padrão: sem incremento, sem teto, janela de 30 dias, ativa")
    void padroes() {
        RegraPenalidade r = fixa(5);

        assertEquals(0, r.getIncrementoPorReincidencia());
        assertNull(r.getTetoPontos());
        assertEquals(30, r.getJanelaReincidenciaDias());
        assertTrue(r.isAtiva());
    }

    @Test
    @DisplayName("Recusa configurações inválidas")
    void validacoes() {
        // nome, tipo, modo e pontos
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade(" ", TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, 5, null, null, null, null));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", null, ModoCalculoPenalidade.FIXA, 5, null, null, null, null));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.NAO_COMPARECIMENTO, null, 5, null, null, null, null));
        assertThrows(RegraNegocioException.class, () -> fixa(0));
        // MANUAL não tem regra
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.MANUAL, ModoCalculoPenalidade.FIXA, 5, null, null, null, null));
        // progressiva exige incremento; fixa não aceita
        assertThrows(RegraNegocioException.class, () -> progressiva(10, 0, null));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, 5, 3, null, null, null));
        // teto e janela
        assertThrows(RegraNegocioException.class, () -> progressiva(10, 5, 9));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, 5, null, null, 0, null));
    }

    @Test
    @DisplayName("Limite de horas é obrigatório para cancelamento tardio e proibido nos outros tipos")
    void limiteDeHoras() {
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.CANCELAMENTO_TARDIO, ModoCalculoPenalidade.FIXA, 5, null, null, null, null));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.CANCELAMENTO_TARDIO, ModoCalculoPenalidade.FIXA, 5, null, null, null, 0));
        assertThrows(RegraNegocioException.class, () -> new RegraPenalidade("x", TipoInfracao.TAREFA_ATRASADA, ModoCalculoPenalidade.FIXA, 5, null, null, null, 2));
        assertDoesNotThrow(() -> new RegraPenalidade("x", TipoInfracao.CANCELAMENTO_TARDIO, ModoCalculoPenalidade.FIXA, 5, null, null, null, 1));
    }

    @Test
    @DisplayName("Pode ser desativada e reativada")
    void ativacao() {
        RegraPenalidade r = fixa(5);

        r.desativar();
        assertFalse(r.isAtiva());
        r.ativar();
        assertTrue(r.isAtiva());
    }
}
