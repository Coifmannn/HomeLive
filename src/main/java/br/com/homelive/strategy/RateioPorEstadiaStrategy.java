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
public class RateioPorEstadiaStrategy implements CalculoRateioStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.POR_ESTADIA;
    }

    @Override
    public List<RateioItem> calcular(Despesa despesa, List<Morador> moradores) {
        if (moradores == null || moradores.isEmpty()) {
            throw new IllegalArgumentException("Não há moradores ativos para ratear a despesa.");
        }

        int totalDias = moradores.stream()
                .mapToInt(m -> m.getDiasEstadia() != null ? m.getDiasEstadia() : 0)
                .sum();

        if (totalDias <= 0) {
            throw new IllegalStateException("O total de dias de estadia de todos os moradores deve ser maior que zero.");
        }

        BigDecimal valorTotal = despesa.getValorTotal();
        List<RateioItem> itens = new ArrayList<>();
        BigDecimal somaCalculada = BigDecimal.ZERO;

        for (int i = 0; i < moradores.size(); i++) {
            Morador morador = moradores.get(i);
            int dias = morador.getDiasEstadia() != null ? morador.getDiasEstadia() : 0;

            BigDecimal proporcao = BigDecimal.valueOf(dias)
                    .divide(BigDecimal.valueOf(totalDias), 6, RoundingMode.HALF_EVEN);
            BigDecimal valorItem = valorTotal.multiply(proporcao).setScale(2, RoundingMode.HALF_EVEN);

            // Ajuste de centavos no último morador
            if (i == moradores.size() - 1) {
                BigDecimal diferenca = valorTotal.subtract(somaCalculada.add(valorItem));
                valorItem = valorItem.add(diferenca);
            }

            somaCalculada = somaCalculada.add(valorItem);
            itens.add(new RateioItem(despesa, morador, valorItem));
        }

        return itens;
    }
}
