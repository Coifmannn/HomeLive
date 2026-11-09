package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import br.com.homelive.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Senha: trocar a própria e redefinir pelo síndico")
class SenhaTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";
    private static final String SENHA = DataInitializer.SENHA_DEMO;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private ResultActions trocar(String usuario, String senhaAtual, String novaSenha) throws Exception {
        return mvc.perform(patch("/api/auth/senha").with(httpBasic(usuario, SENHA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"" + senhaAtual + "\",\"novaSenha\":\"" + novaSenha + "\"}"));
    }

    private int login(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")).andReturn().getResponse().getStatus();
    }

    private Long idDe(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    // ---- trocar a própria senha ------------------------------------------------------

    @Test
    @DisplayName("Trocar a própria senha: a nova entra, a antiga deixa de valer")
    void trocarPropriaSenha() throws Exception {
        trocar(MORADOR, SENHA, "novaSenha1").andExpect(status().isNoContent());

        assertTrue(login(MORADOR, "novaSenha1") == 200, "a senha nova deveria entrar");
        assertTrue(login(MORADOR, SENHA) == 401, "a senha antiga não deveria mais entrar");
    }

    @Test
    @DisplayName("O síndico também troca a própria senha")
    void sindicoTrocaPropriaSenha() throws Exception {
        trocar(SINDICO, SENHA, "outraSenha9").andExpect(status().isNoContent());
        assertTrue(login(SINDICO, "outraSenha9") == 200);
    }

    @Test
    @DisplayName("Senha atual errada: 400 (e não 401, para o front não deslogar a pessoa)")
    void senhaAtualErrada() throws Exception {
        trocar(MORADOR, "errada123", "novaSenha1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A senha atual está incorreta."));
        assertTrue(login(MORADOR, SENHA) == 200, "nada deveria ter mudado");
    }

    @Test
    @DisplayName("Nova senha curta ou igual à atual: 400")
    void novaSenhaInvalida() throws Exception {
        trocar(MORADOR, SENHA, "123").andExpect(status().isBadRequest());
        trocar(MORADOR, SENHA, SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A nova senha deve ser diferente da atual."));
    }

    @Test
    @DisplayName("Campos em branco: 400")
    void camposObrigatorios() throws Exception {
        mvc.perform(patch("/api/auth/senha").with(httpBasic(MORADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sem login, não troca senha: 401")
    void semLogin() throws Exception {
        mvc.perform(patch("/api/auth/senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"x\",\"novaSenha\":\"yyyyyy\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- síndico redefine a senha de um morador --------------------------------------

    @Test
    @DisplayName("Síndico redefine: o morador recebe uma senha temporária e a antiga deixa de valer")
    void sindicoRedefine() throws Exception {
        String resposta = mvc.perform(post("/api/moradores/" + idDe(MORADOR) + "/redefinir-senha")
                        .with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senhaTemporaria").isString())
                .andReturn().getResponse().getContentAsString();

        String temporaria = JsonPath.read(resposta, "$.senhaTemporaria");
        assertTrue(temporaria.matches("[A-HJ-NP-Za-km-z2-9]{8}"), "formato inesperado: " + temporaria);
        assertTrue(login(MORADOR, temporaria) == 200, "a senha temporária deveria entrar");
        assertTrue(login(MORADOR, SENHA) == 401, "a senha antiga não deveria mais entrar");
    }

    @Test
    @DisplayName("Cada redefinição gera uma senha diferente")
    void senhasTemporariasDiferentes() throws Exception {
        String a = JsonPath.read(mvc.perform(post("/api/moradores/" + idDe(MORADOR) + "/redefinir-senha")
                .with(httpBasic(SINDICO, SENHA))).andReturn().getResponse().getContentAsString(), "$.senhaTemporaria");
        String b = JsonPath.read(mvc.perform(post("/api/moradores/" + idDe(MORADOR) + "/redefinir-senha")
                .with(httpBasic(SINDICO, SENHA))).andReturn().getResponse().getContentAsString(), "$.senhaTemporaria");
        assertTrue(!a.equals(b));
    }

    @Test
    @DisplayName("Morador não redefine senha de ninguém: 403")
    void moradorNaoRedefine() throws Exception {
        mvc.perform(post("/api/moradores/" + idDe("sofia.chen@homelive2050.io") + "/redefinir-senha")
                        .with(httpBasic(MORADOR, SENHA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Morador inexistente: 404. O id de um síndico também não vale nessa rota")
    void alvoInvalido() throws Exception {
        mvc.perform(post("/api/moradores/999999/redefinir-senha").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/moradores/" + idDe(SINDICO) + "/redefinir-senha").with(httpBasic(SINDICO, SENHA)))
                .andExpect(status().isNotFound());
    }
}
