package br.com.homelive.event;

import br.com.homelive.model.AgendamentoEspaco;

/** Fato do domínio: o síndico registrou que o morador não usou o espaço que reservou. */
public record MoradorNaoCompareceu(AgendamentoEspaco agendamento) {
}
