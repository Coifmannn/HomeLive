package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateioPorEstadiaStrategy extends RateioProporcionalStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.POR_ESTADIA;
    }

    @Override
    protected BigDecimal peso(Morador morador) {
        Integer dias = morador.getDiasEstadia();
        return BigDecimal.valueOf(dias != null ? dias : 0);
    }

    @Override
    protected String descricaoDoCriterio() {
        return "dias de estadia";
    }
}
