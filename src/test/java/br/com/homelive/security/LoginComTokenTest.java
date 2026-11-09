package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import br.com.homelive.model.Usuario;
import br.com.homelive.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Login com token (continuar conectado)")
class LoginComTokenTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";
    private static final String SENHA = DataInitializer.SENHA_DEMO;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private MvcResult login(String email, String senha, boolean lembrar) throws Exception {
        String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\",\"lembrar\":" + lembrar + "}";
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn();
    }

    private String tokenDe(String email, boolean lembrar) throws Exception {
        MvcResult r = login(email, SENHA, lembrar);
        assertTrue(r.getResponse().getStatus() == 200, "login deveria dar 200");
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("Morador faz login: recebe token, perfil MORADOR e seus dados")
    void loginMorador() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MORADOR + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.papel").value("MORADOR"))
                .andExpect(jsonPath("$.usuario.email").value(MORADOR))
                .andExpect(jsonPath("$.usuario.pontosConvivencia").exists())
                // a resposta nunca devolve a senha nem o hash
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());
    }

    @Test
    @DisplayName("Síndico faz login: papel SINDICO")
    void loginSindico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SINDICO + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("SINDICO"))
                .andExpect(jsonPath("$.usuario.departamento").exists());
    }

    @Test
    @DisplayName("Senha errada e e-mail inexistente dão a mesma resposta 401")
    void credenciaisInvalidas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MORADOR + "\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fantasma@homelive2050.io\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos."));
    }

    @Test
    @DisplayName("Login sem e-mail ou sem senha: 400")
    void camposObrigatorios() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.io\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Conta desativada não consegue fazer login")
    void contaDesativadaNaoEntra() throws Exception {
        Usuario lucas = usuarioRepository.findByEmail(MORADOR).orElseThrow();
        lucas.desativar();
        usuarioRepository.saveAndFlush(lucas);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MORADOR + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("O token abre as rotas da API no lugar da senha")
    void tokenSubstituiSenha() throws Exception {
        String token = tokenDe(MORADOR, false);
        mvc.perform(get("/api/espacos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("O token respeita o perfil: morador leva 403 numa rota de síndico, síndico passa")
    void tokenRespeitaPerfil() throws Exception {
        mvc.perform(get("/api/penalidades").header("Authorization", "Bearer " + tokenDe(MORADOR, false)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/penalidades").header("Authorization", "Bearer " + tokenDe(SINDICO, false)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token adulterado: 401")
    void tokenAdulterado() throws Exception {
        String token = tokenDe(MORADOR, false);
        String adulterado = token.substring(0, token.length() - 6) + "AAAAAA";
        mvc.perform(get("/api/espacos").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token que não é token: 401")
    void tokenLixo() throws Exception {
        mvc.perform(get("/api/espacos").header("Authorization", "Bearer isso-nao-e-um-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token expirado: 401")
    void tokenExpirado() throws Exception {
        String expirado = tokenService.gerar(MORADOR, "MORADOR", Instant.now().minus(Duration.ofHours(2)), Duration.ofHours(1)).valor();
        mvc.perform(get("/api/espacos").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token de quem foi desativado depois do login deixa de valer")
    void tokenDeUsuarioDesativado() throws Exception {
        String token = tokenDe(MORADOR, true);

        Usuario lucas = usuarioRepository.findByEmail(MORADOR).orElseThrow();
        lucas.desativar();
        usuarioRepository.saveAndFlush(lucas);

        mvc.perform(get("/api/espacos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("\"Lembrar de mim\" gera token de ~30 dias; sem marcar, ~1 dia")
    void validadeDoToken() throws Exception {
        MvcResult lembrar = login(MORADOR, SENHA, true);
        MvcResult normal = login(MORADOR, SENHA, false);

        Instant expiraLembrar = Instant.parse(JsonPath.read(lembrar.getResponse().getContentAsString(), "$.expiraEm"));
        Instant expiraNormal = Instant.parse(JsonPath.read(normal.getResponse().getContentAsString(), "$.expiraEm"));

        assertTrue(expiraLembrar.isAfter(Instant.now().plus(Duration.ofDays(29))), "lembrar deveria durar ~30 dias");
        assertTrue(expiraNormal.isBefore(Instant.now().plus(Duration.ofHours(25))), "sem lembrar deveria durar ~1 dia");
        assertTrue(expiraNormal.isAfter(Instant.now().plus(Duration.ofHours(23))), "sem lembrar deveria durar ~1 dia");
    }

    @Test
    @DisplayName("HTTP Basic continua funcionando junto com o token")
    void basicContinuaFuncionando() throws Exception {
        mvc.perform(get("/api/espacos").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic(MORADOR, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("O front (HTML/CSS/JS) abre sem login, mas os dados continuam protegidos")
    void frontPublicoDadosProtegidos() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
        mvc.perform(head("/login.html")).andExpect(status().isOk());
        mvc.perform(get("/manifest.json")).andExpect(status().isOk());
        mvc.perform(get("/sw.js")).andExpect(status().isOk());
        mvc.perform(get("/api/moradores")).andExpect(status().isUnauthorized());
    }
}
