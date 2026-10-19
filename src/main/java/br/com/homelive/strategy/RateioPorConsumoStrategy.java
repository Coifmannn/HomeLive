package br.com.homelive.strategy;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateioPorConsumoStrategy extends RateioProporcionalStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.POR_CONSUMO;
    }

    @Override
    protected BigDecimal peso(Morador morador) {
        Double kwh = morador.getConsumoDeclaradoKwh();
        return BigDecimal.valueOf(kwh != null ? kwh : 0.0);
    }

    @Override
    protected String descricaoDoCriterio() {
        return "consumo declarado";
    }
}
