package br.com.homelive.strategy;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.TipoRodizio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Estratégias de rodízio de tarefas")
class RodizioStrategyTest {

    private Morador m1;
    private Morador m2;
    private Morador m3;

    private final RodizioSequencialStrategy sequencial = new RodizioSequencialStrategy();
    private final RodizioPorPontuacaoStrategy porPontuacao = new RodizioPorPontuacaoStrategy();
    private final RodizioPorCargaStrategy porCarga = new RodizioPorCargaStrategy();

    private static Morador morador(long id) {
        Morador m = new Morador("M" + id, "m" + id + "@test.com", "hash", "1199999000" + id, "10" + id, 30, 100.0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static ContextoRodizio ctx(Long ultimo) {
        return new ContextoRodizio(ultimo, Map.of());
    }

    @BeforeEach
    void setUp() {
        m1 = morador(1);
        m2 = morador(2);
        m3 = morador(3);
    }

    // ---------- Sequencial ----------

    @Test
    @DisplayName("Sequencial: começa pelo primeiro e anda em círculo")
    void sequencialGira() {
        List<Morador> todos = List.of(m1, m2, m3);

        assertSame(m1, sequencial.escolher(todos, ctx(null)));
        assertSame(m2, sequencial.escolher(todos, ctx(1L)));
        assertSame(m3, sequencial.escolher(todos, ctx(2L)));
        assertSame(m1, sequencial.escolher(todos, ctx(3L)));
    }

    @Test
    @DisplayName("Sequencial: sem histórico, começa por quem tem menos tarefas em aberto (empate: menor id)")
    void sequencialPrimeiraVezComecaPeloMenosCarregado() {
        List<Morador> todos = List.of(m1, m2, m3);

        assertSame(m3, sequencial.escolher(todos, new ContextoRodizio(null, Map.of(1L, 2L, 2L, 1L))));
        assertSame(m2, sequencial.escolher(todos, new ContextoRodizio(null, Map.of(1L, 1L, 3L, 1L))));
        assertSame(m1, sequencial.escolher(todos, new ContextoRodizio(null, Map.of())));
    }

    @Test
    @DisplayName("Sequencial: ignora a ordem da lista e pula quem saiu da casa")
    void sequencialPorOrdemDeId() {
        // m2 não está mais ativo; o último foi o m1, então o próximo é o m3.
        assertSame(m3, sequencial.escolher(List.of(m3, m1), ctx(1L)));
        // O último responsável (id 2) nem está mais na lista: segue para o próximo id maior.
        assertSame(m3, sequencial.escolher(List.of(m3, m1), ctx(2L)));
        // Id muito alto: volta ao início.
        assertSame(m1, sequencial.escolher(List.of(m3, m1), ctx(99L)));
    }

    // ---------- Menor pontuação ----------

    @Test
    @DisplayName("Menor pontuação: escolhe quem tem menos pontos de convivência")
    void menorPontuacao() {
        m2.deduzirPontos(30);   // 70
        m3.adicionarPontos(50); // 150

        assertSame(m2, porPontuacao.escolher(List.of(m1, m2, m3), ctx(null)));
    }

    @Test
    @DisplayName("Menor pontuação: no empate evita repetir o último responsável, depois usa o menor id")
    void menorPontuacaoEmpate() {
        m3.adicionarPontos(50); // m1 e m2 empatados com 100

        assertSame(m1, porPontuacao.escolher(List.of(m1, m2, m3), ctx(null)));
        assertSame(m2, porPontuacao.escolher(List.of(m1, m2, m3), ctx(1L)));
        assertSame(m1, porPontuacao.escolher(List.of(m1, m2, m3), ctx(2L)));
    }

    // ---------- Menor carga ----------

    @Test
    @DisplayName("Menor carga: escolhe quem tem menos tarefas em aberto (ausente no mapa = zero)")
    void menorCarga() {
        ContextoRodizio contexto = new ContextoRodizio(null, Map.of(1L, 2L, 3L, 1L));

        assertSame(m2, porCarga.escolher(List.of(m1, m2, m3), contexto));
    }

    @Test
    @DisplayName("Menor carga: empate evita o último responsável")
    void menorCargaEmpate() {
        ContextoRodizio contexto = new ContextoRodizio(2L, Map.of(1L, 1L));

        // m2 e m3 estão com carga zero; como m2 foi o último, vai o m3.
        assertSame(m3, porCarga.escolher(List.of(m1, m2, m3), contexto));
    }

    // ---------- Comuns ----------

    @Test
    @DisplayName("Sem candidatos, toda estratégia recusa")
    void semCandidatos() {
        for (RodizioStrategy s : List.of(sequencial, porPontuacao, porCarga)) {
            assertThrows(RegraNegocioException.class, () -> s.escolher(List.of(), ctx(null)), s.getClass().getSimpleName());
            assertThrows(RegraNegocioException.class, () -> s.escolher(null, ctx(null)), s.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("A factory resolve a estratégia de cada tipo de rodízio")
    void factoryResolveTodosOsTipos() {
        RodizioStrategyFactory factory = new RodizioStrategyFactory(List.of(sequencial, porPontuacao, porCarga));

        assertSame(sequencial, factory.obterStrategy(TipoRodizio.SEQUENCIAL));
        assertSame(porPontuacao, factory.obterStrategy(TipoRodizio.MENOR_PONTUACAO));
        assertSame(porCarga, factory.obterStrategy(TipoRodizio.MENOR_CARGA));
        for (TipoRodizio tipo : TipoRodizio.values()) {
            assertNotNull(factory.obterStrategy(tipo), "falta estratégia para " + tipo);
        }
    }
}
