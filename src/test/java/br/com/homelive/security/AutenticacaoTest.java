package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Autenticação HTTP Basic")
class AutenticacaoTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sem credenciais: 401")
    void semLogin() throws Exception {
        mvc.perform(get("/api/espacos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Senha errada: 401")
    void senhaErrada() throws Exception {
        mvc.perform(get("/api/espacos").with(httpBasic(MORADOR, "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuário inexistente: 401")
    void usuarioInexistente() throws Exception {
        mvc.perform(get("/api/espacos").with(httpBasic("fantasma@homelive2050.io", DataInitializer.SENHA_DEMO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Síndico e morador com senha correta entram: 200")
    void loginValido() throws Exception {
        mvc.perform(get("/api/espacos").with(httpBasic(SINDICO, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/espacos").with(httpBasic(MORADOR, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Console do H2 é restrito ao síndico")
    void h2ConsoleSoSindico() throws Exception {
        mvc.perform(get("/h2-console/").with(httpBasic(MORADOR, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isForbidden());
    }
}
