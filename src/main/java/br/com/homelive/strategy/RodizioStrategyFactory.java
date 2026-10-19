package br.com.homelive.strategy;

import br.com.homelive.model.enums.TipoRodizio;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RodizioStrategyFactory {

    private final Map<TipoRodizio, RodizioStrategy> strategies = new EnumMap<>(TipoRodizio.class);

    public RodizioStrategyFactory(List<RodizioStrategy> strategyList) {
        for (RodizioStrategy strategy : strategyList) {
            strategies.put(strategy.getTipoRodizio(), strategy);
        }
    }

    public RodizioStrategy obterStrategy(TipoRodizio tipo) {
        RodizioStrategy strategy = strategies.get(tipo);
        if (strategy == null) {
            throw new IllegalArgumentException("Estratégia de rodízio não encontrada para o tipo: " + tipo);
        }
        return strategy;
    }
}
