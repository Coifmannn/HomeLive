package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.TipoInfracao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Penalidade aplicada")
class PenalidadeTest {

    private Morador lucas;
    private Sindico helena;

    @BeforeEach
    void setUp() {
        lucas = new Morador("Lucas", "lucas@test.com", "hash", "11999990001", "101", 30);
        helena = new Sindico("Helena", "helena@test.com", "hash", "11999990000", "Governança");
    }

    @Test
    @DisplayName("Aplicar registra a penalidade e desconta os pontos do morador")
    void aplicaEDesconta() {
        Penalidade p = Penalidade.aplicar(lucas, null, TipoInfracao.MANUAL, 30, "Barulho após as 22h", helena);

        assertEquals(70, lucas.getPontosConvivencia());
        assertEquals(30, p.getPontos());
        assertSame(lucas, p.getMorador());
        assertSame(helena, p.getAplicadaPor());
        assertEquals(TipoInfracao.MANUAL, p.getTipoInfracao());
        assertNotNull(p.getDataAplicacao());
    }

    @Test
    @DisplayName("Os pontos do morador nunca ficam negativos")
    void naoFicaNegativo() {
        Penalidade.aplicar(lucas, null, TipoInfracao.MANUAL, 500, "Grave", helena);

        assertEquals(0, lucas.getPontosConvivencia());
    }

    @Test
    @DisplayName("Recusa dados inválidos sem descontar pontos")
    void validacoes() {
        assertThrows(RegraNegocioException.class, () -> Penalidade.aplicar(null, null, TipoInfracao.MANUAL, 5, "x", helena));
        assertThrows(RegraNegocioException.class, () -> Penalidade.aplicar(lucas, null, null, 5, "x", helena));
        assertThrows(RegraNegocioException.class, () -> Penalidade.aplicar(lucas, null, TipoInfracao.MANUAL, 0, "x", helena));
        assertThrows(RegraNegocioException.class, () -> Penalidade.aplicar(lucas, null, TipoInfracao.MANUAL, 5, " ", helena));
        assertThrows(RegraNegocioException.class, () -> Penalidade.aplicar(lucas, null, TipoInfracao.MANUAL, 5, null, helena));

        assertEquals(100, lucas.getPontosConvivencia());
    }

    @Test
    @DisplayName("Penalidade automática não tem síndico associado")
    void automatica() {
        Penalidade p = Penalidade.aplicar(lucas, null, TipoInfracao.TAREFA_ATRASADA, 10, "Lixo", null);

        assertNull(p.getAplicadaPor());
    }
}
