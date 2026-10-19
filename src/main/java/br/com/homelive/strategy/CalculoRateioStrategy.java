package br.com.homelive.strategy;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.enums.TipoRateio;

import java.util.List;

/**
 * Interface Strategy para os diferentes algoritmos de divisão de custos e despesas.
 */
public interface CalculoRateioStrategy {

    TipoRateio getTipoRateio();

    List<RateioItem> calcular(Despesa despesa, List<Morador> moradores, ContextoRateio contexto);
}
