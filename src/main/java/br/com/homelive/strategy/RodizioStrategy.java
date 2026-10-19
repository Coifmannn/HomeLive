package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRodizio;

import java.util.List;

/** Strategy: decide qual morador recebe a próxima tarefa de uma escala. */
public interface RodizioStrategy {

    TipoRodizio getTipoRodizio();

    Morador escolher(List<Morador> candidatos, ContextoRodizio contexto);
}
