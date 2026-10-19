package br.com.homelive.strategy;

import br.com.homelive.model.Morador;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Template Method: escolhe quem tem a menor "medida". As subclasses só dizem qual é a medida.
 * Empate: evita repetir o último responsável; persistindo, vence o menor id (resultado determinístico).
 */
public abstract class RodizioPorMenorMedidaStrategy extends RodizioBaseStrategy {

    protected abstract long medida(Morador morador, ContextoRodizio contexto);

    @Override
    protected final Morador selecionar(List<Morador> candidatos, ContextoRodizio contexto) {
        Long ultimoId = contexto.ultimoResponsavelId();
        return candidatos.stream()
                .min(Comparator
                        .comparingLong((Morador m) -> medida(m, contexto))
                        .thenComparing((Morador m) -> Objects.equals(m.getId(), ultimoId))
                        .thenComparing(Morador::getId))
                .orElseThrow();
    }
}
