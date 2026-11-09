package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import br.com.homelive.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Gestão de moradores: reativar e editar estadia")
class GestaoDeMoradoresTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";
    private static final String OUTRO = "sofia.chen@homelive2050.io";
    private static final String SENHA = DataInitializer.SENHA_DEMO;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long idDe(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    private int login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}")).andReturn().getResponse().getStatus();
    }

    // ---- reativar --------------------------------------------------------------------

    @Test
    @DisplayName("Morador desativado não entra; depois de reativado pelo síndico, entra de novo")
    void desativarEReativar() throws Exception {
        Long id = idDe(MORADOR);

        mvc.perform(delete("/api/moradores/" + id).with(httpBasic(SINDICO, SENHA))).andExpect(status().isNoContent());
        assertEquals(401, login(MORADOR), "desativado não deveria entrar");

        mvc.perform(patch("/api/moradores/" + id + "/reativar").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNoContent());
        assertEquals(200, login(MORADOR), "reativado deveria entrar");
    }

    @Test
    @DisplayName("Reativar volta a contar o morador no painel")
    void reativarVoltaAoPainel() throws Exception {
        long antes = contarMoradoresNoPainel();
        Long id = idDe(MORADOR);

        mvc.perform(delete("/api/moradores/" + id).with(httpBasic(SINDICO, SENHA))).andExpect(status().isNoContent());
        assertEquals(antes - 1, contarMoradoresNoPainel());

        mvc.perform(patch("/api/moradores/" + id + "/reativar").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNoContent());
        assertEquals(antes, contarMoradoresNoPainel());
    }

    private long contarMoradoresNoPainel() throws Exception {
        String json = mvc.perform(get("/api/dashboard").with(httpBasic(SINDICO, SENHA)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.totalMoradores")).longValue();
    }

    @Test
    @DisplayName("Reativar quem já está ativo não dá erro (resultado é o mesmo)")
    void reativarJaAtivo() throws Exception {
        mvc.perform(patch("/api/moradores/" + idDe(MORADOR) + "/reativar").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNoContent());
        assertEquals(200, login(MORADOR));
    }

    @Test
    @DisplayName("Reativar morador que não existe: 404")
    void reativarInexistente() throws Exception {
        mvc.perform(patch("/api/moradores/999999/reativar").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Morador não reativa ninguém: 403")
    void moradorNaoReativa() throws Exception {
        mvc.perform(patch("/api/moradores/" + idDe(OUTRO) + "/reativar").with(httpBasic(MORADOR, SENHA)))
                .andExpect(status().isForbidden());
    }

    // ---- estadia ---------------------------------------------------------------------

    @Test
    @DisplayName("Síndico altera os dias de estadia e a resposta já mostra o novo valor")
    void editarEstadia() throws Exception {
        mvc.perform(patch("/api/moradores/" + idDe(MORADOR) + "/estadia").param("dias", "45")
                        .with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasEstadia").value(45));
    }

    @Test
    @DisplayName("Estadia zero é aceita; negativa é recusada (400)")
    void limitesDaEstadia() throws Exception {
        Long id = idDe(MORADOR);
        mvc.perform(patch("/api/moradores/" + id + "/estadia").param("dias", "0").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.diasEstadia").value(0));
        mvc.perform(patch("/api/moradores/" + id + "/estadia").param("dias", "-3").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sem informar os dias, ou com texto no lugar do número: 400")
    void estadiaInvalida() throws Exception {
        Long id = idDe(MORADOR);
        mvc.perform(patch("/api/moradores/" + id + "/estadia").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/moradores/" + id + "/estadia").param("dias", "abc").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isBadRequest());
    }
}
