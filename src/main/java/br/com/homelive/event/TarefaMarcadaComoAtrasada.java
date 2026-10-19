package br.com.homelive.event;

import br.com.homelive.model.TarefaDomestica;

/** Fato do domínio: uma tarefa passou do prazo sem ser cumprida. Quem quiser reagir (penalidade, aviso...) escuta. */
public record TarefaMarcadaComoAtrasada(TarefaDomestica tarefa) {
}
