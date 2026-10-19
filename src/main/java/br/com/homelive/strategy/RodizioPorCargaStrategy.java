package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRodizio;
import org.springframework.stereotype.Component;

@Component
public class RodizioPorCargaStrategy extends RodizioPorMenorMedidaStrategy {

    @Override
    public TipoRodizio getTipoRodizio() {
        return TipoRodizio.MENOR_CARGA;
    }

    @Override
    protected long medida(Morador morador, ContextoRodizio contexto) {
        return contexto.tarefasAbertas(morador.getId());
    }
}
