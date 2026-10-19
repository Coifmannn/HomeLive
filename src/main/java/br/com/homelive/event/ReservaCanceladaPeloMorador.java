package br.com.homelive.event;

import br.com.homelive.model.AgendamentoEspaco;

import java.time.LocalDateTime;

/** Fato do domínio: o próprio morador cancelou a reserva (cancelamento feito pelo síndico não gera este evento). */
public record ReservaCanceladaPeloMorador(AgendamentoEspaco agendamento, LocalDateTime quando) {
}
