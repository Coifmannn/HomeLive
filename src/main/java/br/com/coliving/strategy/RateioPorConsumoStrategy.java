package br.com.coliving.strategy;

import br.com.coliving.model.Despesa;
import br.com.coliving.model.Morador;
import br.com.coliving.model.RateioItem;
import br.com.coliving.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class RateioPorConsumoStrategy implements CalculoRateioStrategy {

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.POR_CONSUMO;
    }

    @Override
    public List<RateioItem> calcular(Despesa despesa, List<Morador> moradores) {
        if (moradores == null || moradores.isEmpty()) {
            throw new IllegalArgumentException("Não há moradores ativos para ratear a despesa.");
        }

        double totalConsumo = moradores.stream()
                .mapToDouble(m -> m.getConsumoDeclaradoKwh() != null ? m.getConsumoDeclaradoKwh() : 0.0)
                .sum();

        if (totalConsumo <= 0.0) {
            throw new IllegalStateException("O total de consumo declarado de todos os moradores deve ser maior que zero.");
        }

        BigDecimal valorTotal = despesa.getValorTotal();
        List<RateioItem> itens = new ArrayList<>();
        BigDecimal somaCalculada = BigDecimal.ZERO;

        for (int i = 0; i < moradores.size(); i++) {
            Morador morador = moradores.get(i);
            double consumo = morador.getConsumoDeclaradoKwh() != null ? morador.getConsumoDeclaradoKwh() : 0.0;

            BigDecimal proporcao = BigDecimal.valueOf(consumo)
                    .divide(BigDecimal.valueOf(totalConsumo), 6, RoundingMode.HALF_EVEN);
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
