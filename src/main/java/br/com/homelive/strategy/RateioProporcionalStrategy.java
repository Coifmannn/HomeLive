package br.com.homelive.strategy;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Template Method sobre o Strategy: todo rateio é proporcional a um "peso" por morador.
 * <p>
 * O esqueleto do algoritmo (validar, somar pesos, repartir e fechar os centavos) vive aqui e é
 * {@code final}. As subclasses decidem apenas <b>qual é o peso</b> de cada morador:
 * igualitário = 1, estadia = dias, consumo = kWh.
 */
public abstract class RateioProporcionalStrategy implements CalculoRateioStrategy {

    /** Peso do morador neste critério de rateio. Nunca deve ser negativo. */
    protected abstract BigDecimal peso(Morador morador, ContextoRateio contexto);

    /** Gancho: critérios que dependem de dados externos conferem aqui se eles estão completos. */
    protected void validar(Despesa despesa, List<Morador> moradores, ContextoRateio contexto) {
    }

    /** Nome do critério, usado na mensagem de erro quando a soma dos pesos é zero. */
    protected abstract String descricaoDoCriterio();

    @Override
    public final List<RateioItem> calcular(Despesa despesa, List<Morador> moradores, ContextoRateio contexto) {
        if (moradores == null || moradores.isEmpty()) {
            throw new RegraNegocioException("Não há moradores ativos para ratear a despesa.");
        }
        validar(despesa, moradores, contexto);

        BigDecimal somaPesos = moradores.stream()
                .map(m -> peso(m, contexto))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (somaPesos.signum() <= 0) {
            throw new RegraNegocioException(
                    "O total de " + descricaoDoCriterio() + " de todos os moradores deve ser maior que zero.");
        }

        BigDecimal valorTotal = despesa.getValorTotal();
        List<RateioItem> itens = new ArrayList<>();
        BigDecimal somaCalculada = BigDecimal.ZERO;

        for (int i = 0; i < moradores.size(); i++) {
            Morador morador = moradores.get(i);
            boolean ultimo = i == moradores.size() - 1;

            // O último absorve a diferença de arredondamento para a soma fechar em 100% do valor total.
            BigDecimal valorItem = ultimo
                    ? valorTotal.subtract(somaCalculada)
                    : valorTotal.multiply(peso(morador, contexto)).divide(somaPesos, 2, RoundingMode.HALF_EVEN);

            somaCalculada = somaCalculada.add(valorItem);
            itens.add(new RateioItem(despesa, morador, valorItem));
        }

        return itens;
    }
}
