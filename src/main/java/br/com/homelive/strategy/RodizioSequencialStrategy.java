package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRodizio;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Em ordem circular de id: o primeiro morador com id maior que o do último responsável; senão volta ao início.
 * Sem histórico, parte de quem tem menos tarefas em aberto.
 */
@Component
public class RodizioSequencialStrategy extends RodizioBaseStrategy {

    @Override
    public TipoRodizio getTipoRodizio() {
        return TipoRodizio.SEQUENCIAL;
    }

    @Override
    protected Morador selecionar(List<Morador> candidatos, ContextoRodizio contexto) {
        List<Morador> emOrdem = candidatos.stream().sorted(Comparator.comparing(Morador::getId)).toList();
        Long ultimoId = contexto.ultimoResponsavelId();
        if (ultimoId == null) {
            // Primeira vez deste modelo: parte de quem tem menos tarefas em aberto (empate: menor id).
            // Se todo modelo começasse pelo primeiro morador, os rodízios girariam em sincronia e a
            // mesma pessoa pegaria todas as tarefas da semana.
            return emOrdem.stream()
                    .min(Comparator.comparingLong(m -> contexto.tarefasAbertas(m.getId())))
                    .orElseThrow();
        }
        return emOrdem.stream()
                .filter(m -> m.getId() > ultimoId)
                .findFirst()
                .orElse(emOrdem.get(0));
    }
}
