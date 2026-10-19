package br.com.homelive.strategy;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;

import java.util.List;

/** Template Method: valida os candidatos uma vez; cada estratégia só implementa {@link #selecionar}. */
public abstract class RodizioBaseStrategy implements RodizioStrategy {

    protected abstract Morador selecionar(List<Morador> candidatos, ContextoRodizio contexto);

    @Override
    public final Morador escolher(List<Morador> candidatos, ContextoRodizio contexto) {
        if (candidatos == null || candidatos.isEmpty()) {
            throw new RegraNegocioException("Não há moradores ativos para o rodízio de tarefas.");
        }
        return selecionar(candidatos, contexto);
    }
}
