package br.com.homelive.model.enums;

public enum TipoRodizio {
    /** Cada vez um morador, em ordem circular. */
    SEQUENCIAL,
    /** Quem tem menos pontos de convivência (ajuda a equilibrar o ranking). */
    MENOR_PONTUACAO,
    /** Quem tem menos tarefas em aberto no momento. */
    MENOR_CARGA
}
