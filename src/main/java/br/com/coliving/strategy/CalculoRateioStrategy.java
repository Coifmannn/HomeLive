package br.com.coliving.strategy;

import br.com.coliving.model.Despesa;
import br.com.coliving.model.Morador;
import br.com.coliving.model.RateioItem;
import br.com.coliving.model.enums.TipoRateio;

import java.util.List;

/**
 * Interface Strategy para os diferentes algoritmos de divisão de custos e despesas.
 */
public interface CalculoRateioStrategy {

    TipoRateio getTipoRateio();

    List<RateioItem> calcular(Despesa despesa, List<Morador> moradores);
}
