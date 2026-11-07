package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.StatusMembro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entrada de membros na casa")
class CasaMembroTest {

    private Casa casa() {
        return new Casa("República", "HL-TEST");
    }

    private Morador recemCadastrado() {
        return new Morador("Ana", "ana@x.com", "hash", null);
    }

    @Test
    @DisplayName("Recém-cadastrado não tem casa nem quarto")
    void nasceSemCasa() {
        Morador m = recemCadastrado();
        assertNull(m.getCasa());
        assertNull(m.getNumeroQuarto());
        assertEquals(StatusMembro.SEM_CASA, m.getStatusMembro());
    }

    @Test
    @DisplayName("Pedir para entrar deixa o morador PENDENTE, já com casa e quarto")
    void solicitarEntrada() {
        Morador m = recemCadastrado();
        Casa c = casa();
        m.solicitarEntrada(c, " Suíte 3 ");
        assertSame(c, m.getCasa());
        assertEquals("Suíte 3", m.getNumeroQuarto());
        assertEquals(StatusMembro.PENDENTE, m.getStatusMembro());
    }

    @Test
    @DisplayName("Entrada sem quarto é recusada")
    void quartoObrigatorio() {
        Morador m = recemCadastrado();
        assertThrows(RegraNegocioException.class, () -> m.solicitarEntrada(casa(), "  "));
        assertNull(m.getCasa());
    }

    @Test
    @DisplayName("Aprovar torna ATIVO")
    void aprovar() {
        Morador m = recemCadastrado();
        m.solicitarEntrada(casa(), "101");
        m.aprovarEntrada();
        assertEquals(StatusMembro.ATIVO, m.getStatusMembro());
    }

    @Test
    @DisplayName("Recusar devolve a SEM_CASA, livre para tentar outro código")
    void recusar() {
        Morador m = recemCadastrado();
        m.solicitarEntrada(casa(), "101");
        m.recusarEntrada();
        assertNull(m.getCasa());
        assertNull(m.getNumeroQuarto());
        assertEquals(StatusMembro.SEM_CASA, m.getStatusMembro());
        m.solicitarEntrada(new Casa("Outra", "HL-OUTR"), "202");
        assertEquals(StatusMembro.PENDENTE, m.getStatusMembro());
    }

    @Test
    @DisplayName("Só dá para aprovar ou recusar quem está PENDENTE")
    void transicoesInvalidas() {
        Morador semCasa = recemCadastrado();
        assertThrows(RegraNegocioException.class, semCasa::aprovarEntrada);
        assertThrows(RegraNegocioException.class, semCasa::recusarEntrada);

        Morador ativo = recemCadastrado();
        ativo.solicitarEntrada(casa(), "101");
        ativo.aprovarEntrada();
        assertThrows(RegraNegocioException.class, ativo::aprovarEntrada);
        assertThrows(RegraNegocioException.class, ativo::recusarEntrada);
    }

    @Test
    @DisplayName("Uma pessoa não pode estar em duas casas")
    void umaCasaPorPessoa() {
        Morador m = recemCadastrado();
        m.solicitarEntrada(casa(), "101");
        assertThrows(RegraNegocioException.class,
                () -> m.solicitarEntrada(new Casa("Outra", "HL-OUTR"), "202"));
        assertThrows(RegraNegocioException.class, () -> m.entrarNaCasa(new Casa("Outra", "HL-OUTR")));
    }

    @Test
    @DisplayName("Síndico com casa responde ATIVO (polimorfismo); sem casa, SEM_CASA")
    void statusDoSindico() {
        Sindico s = new Sindico("Bia", "bia@x.com", "hash", null, null);
        assertEquals(StatusMembro.SEM_CASA, s.getStatusMembro());
        s.entrarNaCasa(casa());
        assertEquals(StatusMembro.ATIVO, s.getStatusMembro());
    }

    @Test
    @DisplayName("Casa exige nome e código")
    void casaValida() {
        assertThrows(RegraNegocioException.class, () -> new Casa(" ", "HL-AAAA"));
        assertThrows(RegraNegocioException.class, () -> new Casa("Casa", null));
    }
}
