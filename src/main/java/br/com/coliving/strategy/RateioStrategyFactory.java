package br.com.coliving.strategy;

import br.com.coliving.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RateioStrategyFactory {

    private final Map<TipoRateio, CalculoRateioStrategy> strategies = new EnumMap<>(TipoRateio.class);

    public RateioStrategyFactory(List<CalculoRateioStrategy> strategyList) {
        for (CalculoRateioStrategy strategy : strategyList) {
            strategies.put(strategy.getTipoRateio(), strategy);
        }
    }

    public CalculoRateioStrategy obterStrategy(TipoRateio tipoRateio) {
        CalculoRateioStrategy strategy = strategies.get(tipoRateio);
        if (strategy == null) {
            throw new IllegalArgumentException("Estratégia de rateio não encontrada para o tipo: " + tipoRateio);
        }
        return strategy;
    }
}
