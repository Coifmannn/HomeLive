package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Casa: nome e código de convite")
class CasaTest {

    private static final String FORMATO = "HL-[A-HJ-NP-Z2-9]{6}";

    @Test
    @DisplayName("Código de convite tem o formato HL-XXXXXX, sem letras confundíveis (I, O, 0, 1)")
    void formatoDoCodigo() {
        for (int i = 0; i < 500; i++) {
            String codigo = new Casa("Casa Teste").getCodigoConvite();
            assertTrue(codigo.matches(FORMATO), "formato inesperado: " + codigo);
        }
    }

    @Test
    @DisplayName("Códigos gerados não se repetem na prática")
    void codigosDiferentes() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            codigos.add(new Casa("Casa Teste").getCodigoConvite());
        }
        assertEquals(500, codigos.size());
    }

    @Test
    @DisplayName("O código aceita minúsculas e espaços nas pontas")
    void aceitaCodigoSemRigorDeCaixa() {
        Casa casa = new Casa("Casa Teste");
        String codigo = casa.getCodigoConvite();

        assertTrue(casa.aceitaCodigo(codigo));
        assertTrue(casa.aceitaCodigo("  " + codigo.toLowerCase() + "  "));
    }

    @Test
    @DisplayName("Código errado, vazio ou nulo é recusado")
    void recusaCodigoErrado() {
        Casa casa = new Casa("Casa Teste");

        assertFalse(casa.aceitaCodigo("HL-AAAAAA".equals(casa.getCodigoConvite()) ? "HL-BBBBBB" : "HL-AAAAAA"));
        assertFalse(casa.aceitaCodigo(""));
        assertFalse(casa.aceitaCodigo("   "));
        assertFalse(casa.aceitaCodigo(null));
    }

    @Test
    @DisplayName("Renovar troca o código: o antigo deixa de valer")
    void renovarInvalidaOAntigo() {
        Casa casa = new Casa("Casa Teste");
        String antigo = casa.getCodigoConvite();

        casa.renovarCodigo();

        assertNotEquals(antigo, casa.getCodigoConvite());
        assertFalse(casa.aceitaCodigo(antigo));
        assertTrue(casa.aceitaCodigo(casa.getCodigoConvite()));
    }

    @Test
    @DisplayName("Nome é obrigatório, sem espaços nas pontas e com no máximo 80 caracteres")
    void validaNome() {
        assertThrows(RegraNegocioException.class, () -> new Casa(null));
        assertThrows(RegraNegocioException.class, () -> new Casa("   "));
        assertThrows(RegraNegocioException.class, () -> new Casa("x".repeat(81)));

        assertEquals("Casa Aurora", new Casa("  Casa Aurora  ").getNome());
        assertEquals(80, new Casa("x".repeat(80)).getNome().length());
    }
}
