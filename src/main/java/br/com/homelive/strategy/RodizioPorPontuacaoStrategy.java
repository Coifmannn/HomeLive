package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRodizio;
import org.springframework.stereotype.Component;

@Component
public class RodizioPorPontuacaoStrategy extends RodizioPorMenorMedidaStrategy {

    @Override
    public TipoRodizio getTipoRodizio() {
        return TipoRodizio.MENOR_PONTUACAO;
    }

    @Override
    protected long medida(Morador morador, ContextoRodizio contexto) {
        return morador.getPontosConvivencia();
    }
}
