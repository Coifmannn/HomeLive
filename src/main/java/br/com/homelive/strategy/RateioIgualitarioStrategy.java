package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateioIgualitarioStrategy extends RateioProporcionalStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.IGUALITARIO;
    }

    @Override
    protected BigDecimal peso(Morador morador) {
        return BigDecimal.ONE;
    }

    @Override
    protected String descricaoDoCriterio() {
        return "moradores";
    }
}
