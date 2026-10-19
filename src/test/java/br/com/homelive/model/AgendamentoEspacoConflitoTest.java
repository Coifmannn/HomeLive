package br.com.homelive.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes de Validação POO de Conflito de Agendamentos")
class AgendamentoEspacoConflitoTest {

    private EspacoComum espaco;
    private Morador morador;

    @BeforeEach
    void setUp() {
        espaco = new EspacoComum("Escritório Acústico", "Sala silenciosa", 4, "Silêncio");
        morador = new Morador("Lucas", "lucas@test.com", "11999990001", "101", 30, 100.0);
    }

    private AgendamentoEspaco reserva(LocalDateTime inicio, LocalDateTime fim) {
        return new AgendamentoEspaco(espaco, morador, inicio, fim, "teste");
    }

    @Test
    @DisplayName("Deve detectar sobreposição exata de horários")
    void testSobreposicaoExata() {
        LocalDateTime inicio = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fim = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoExistente = reserva(inicio, fim);

        assertTrue(agendamentoExistente.conflitaCom(inicio, fim));
    }

    @Test
    @DisplayName("Deve detectar sobreposição parcial quando novo início cai no meio da reserva existente")
    void testSobreposicaoParcial() {
        LocalDateTime inicioExistente = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fimExistente = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoExistente = reserva(inicioExistente, fimExistente);

        // Novo agendamento de 15:00 às 17:00 (conflita entre 15:00 e 16:00)
        LocalDateTime novoInicio = LocalDateTime.of(2050, 10, 20, 15, 0);
        LocalDateTime novoFim = LocalDateTime.of(2050, 10, 20, 17, 0);

        assertTrue(agendamentoExistente.conflitaCom(novoInicio, novoFim));
    }

    @Test
    @DisplayName("Não deve detectar conflito para horários adjacentes (fim de um é início do outro)")
    void testHorariosAdjacentes() {
        LocalDateTime inicioExistente = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fimExistente = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoExistente = reserva(inicioExistente, fimExistente);

        // Novo agendamento exatamente às 16:00 até 18:00
        LocalDateTime novoInicio = LocalDateTime.of(2050, 10, 20, 16, 0);
        LocalDateTime novoFim = LocalDateTime.of(2050, 10, 20, 18, 0);

        assertFalse(agendamentoExistente.conflitaCom(novoInicio, novoFim));
    }

    @Test
    @DisplayName("Agendamento cancelado não deve gerar conflito")
    void testAgendamentoCanceladoNaoGeraConflito() {
        LocalDateTime inicio = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fim = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoCancelado = reserva(inicio, fim);
        agendamentoCancelado.cancelar();

        assertFalse(agendamentoCancelado.conflitaCom(inicio, fim));
    }
}
