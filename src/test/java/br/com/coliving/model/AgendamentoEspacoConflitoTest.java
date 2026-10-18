package br.com.coliving.model;

import br.com.coliving.model.enums.StatusAgendamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes de Validação POO de Conflito de Agendamentos")
class AgendamentoEspacoConflitoTest {

    @Test
    @DisplayName("Deve detectar sobreposição exata de horários")
    void testSobreposicaoExata() {
        LocalDateTime inicio = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fim = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoExistente = new AgendamentoEspaco();
        agendamentoExistente.setDataHoraInicio(inicio);
        agendamentoExistente.setDataHoraFim(fim);
        agendamentoExistente.setStatus(StatusAgendamento.CONFIRMADO);

        assertTrue(agendamentoExistente.conflitaCom(inicio, fim));
    }

    @Test
    @DisplayName("Deve detectar sobreposição parcial quando novo início cai no meio da reserva existente")
    void testSobreposicaoParcial() {
        LocalDateTime inicioExistente = LocalDateTime.of(2050, 10, 20, 14, 0);
        LocalDateTime fimExistente = LocalDateTime.of(2050, 10, 20, 16, 0);

        AgendamentoEspaco agendamentoExistente = new AgendamentoEspaco();
        agendamentoExistente.setDataHoraInicio(inicioExistente);
        agendamentoExistente.setDataHoraFim(fimExistente);
        agendamentoExistente.setStatus(StatusAgendamento.CONFIRMADO);

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

        AgendamentoEspaco agendamentoExistente = new AgendamentoEspaco();
        agendamentoExistente.setDataHoraInicio(inicioExistente);
        agendamentoExistente.setDataHoraFim(fimExistente);
        agendamentoExistente.setStatus(StatusAgendamento.CONFIRMADO);

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

        AgendamentoEspaco agendamentoCancelado = new AgendamentoEspaco();
        agendamentoCancelado.setDataHoraInicio(inicio);
        agendamentoCancelado.setDataHoraFim(fim);
        agendamentoCancelado.setStatus(StatusAgendamento.CANCELADO);

        assertFalse(agendamentoCancelado.conflitaCom(inicio, fim));
    }
}
