package br.com.homelive.strategy;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class RateioIgualitarioStrategy implements CalculoRateioStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.IGUALITARIO;
    }

    @Override
    public List<RateioItem> calcular(Despesa despesa, List<Morador> moradores) {
        if (moradores == null || moradores.isEmpty()) {
            throw new IllegalArgumentException("Não há moradores ativos para ratear a despesa.");
        }

        BigDecimal valorTotal = despesa.getValorTotal();
        int totalMoradores = moradores.size();

        BigDecimal valorPorMorador = valorTotal.divide(BigDecimal.valueOf(totalMoradores), 2, RoundingMode.HALF_EVEN);
        List<RateioItem> itens = new ArrayList<>();
        BigDecimal somaCalculada = BigDecimal.ZERO;

        for (int i = 0; i < totalMoradores; i++) {
            Morador morador = moradores.get(i);
            BigDecimal valorItem = valorPorMorador;

            // Ajuste de centavos na última parcela para fechar 100% com o valorTotal
            if (i == totalMoradores - 1) {
                BigDecimal diferenca = valorTotal.subtract(somaCalculada.add(valorItem));
                valorItem = valorItem.add(diferenca);
            }

            somaCalculada = somaCalculada.add(valorItem);
            itens.add(new RateioItem(despesa, morador, valorItem));
        }

        return itens;
    }
}
