package br.com.homelive.strategy;

import java.util.Map;

/**
 * Dados de que as estratégias de rodízio precisam, entregues prontos pelo service:
 * assim elas continuam funções puras, sem acesso a banco.
 *
 * @param ultimoResponsavelId       quem recebeu a última tarefa deste modelo (null se nunca houve)
 * @param tarefasAbertasPorMorador  quantidade de tarefas em aberto por id de morador
 */
public record ContextoRodizio(Long ultimoResponsavelId, Map<Long, Long> tarefasAbertasPorMorador) {

    public long tarefasAbertas(Long moradorId) {
        return tarefasAbertasPorMorador.getOrDefault(moradorId, 0L);
    }
}
