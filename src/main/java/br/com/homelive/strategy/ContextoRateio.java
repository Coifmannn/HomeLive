package br.com.homelive.strategy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Dados de fora do morador que o rateio pode precisar, entregues prontos pelo service para que as
 * estratégias continuem puras (sem acesso a banco).
 *
 * @param consumoKwhPorMorador leitura de consumo, por id de morador, do mês (competência) da despesa
 */
public record ContextoRateio(Map<Long, BigDecimal> consumoKwhPorMorador) {

    public static ContextoRateio vazio() {
        return new ContextoRateio(Map.of());
    }

    public Optional<BigDecimal> consumo(Long moradorId) {
        return Optional.ofNullable(consumoKwhPorMorador.get(moradorId));
    }
}
