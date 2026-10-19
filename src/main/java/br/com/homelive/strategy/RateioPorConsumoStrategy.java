package br.com.homelive.strategy;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Despesa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Peso = leitura de consumo (kWh) do mês da despesa. Sem leitura de algum morador, o rateio é recusado. */
@Component
public class RateioPorConsumoStrategy extends RateioProporcionalStrategy {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    @Override
    public TipoRateio getTipoRateio() {
        return TipoRateio.POR_CONSUMO;
    }

    @Override
    protected void validar(Despesa despesa, List<Morador> moradores, ContextoRateio contexto) {
        List<String> semLeitura = moradores.stream()
                .filter(m -> contexto.consumo(m.getId()).isEmpty())
                .map(Morador::getNome)
                .toList();
        if (!semLeitura.isEmpty()) {
            throw new RegraNegocioException("Faltam leituras de consumo de " + despesa.getMesCompetencia().format(MES)
                    + " para: " + String.join(", ", semLeitura) + ".");
        }
    }

    @Override
    protected BigDecimal peso(Morador morador, ContextoRateio contexto) {
        return contexto.consumo(morador.getId()).orElse(BigDecimal.ZERO);
    }

    @Override
    protected String descricaoDoCriterio() {
        return "consumo";
    }
}
