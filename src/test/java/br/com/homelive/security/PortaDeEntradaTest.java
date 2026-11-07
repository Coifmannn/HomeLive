package br.com.homelive.security;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quem não é membro ativo de uma casa não enxerga os dados dela. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Porta de entrada: só membro ativo de uma casa acessa os dados")
class PortaDeEntradaTest {

    private static final String SENHA = "segredo123";
    private static final String SEM_CASA = "semcasa@teste.io";
    private static final String PENDENTE = "pendente@teste.io";
    private static final String ATIVO = "lucas.silva@homelive2050.io";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void criarUsuarios() throws Exception {
        for (String email : new String[]{SEM_CASA, PENDENTE}) {
            mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/casas/entrar").with(httpBasic(PENDENTE, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoConvite\":\"HL-DEMO\",\"numeroQuarto\":\"9\"}"))
                .andExpect(status().isOk());
    }

    private int chamar(String metodo, String url, String usuario, String senha) throws Exception {
        return mvc.perform(request(HttpMethod.valueOf(metodo), url)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(httpBasic(usuario, senha)))
                .andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1}: sem casa e pendente levam 403")
    @DisplayName("Rotas de dados ficam fechadas para SEM_CASA e PENDENTE")
    @CsvSource({
            "GET,  /api/dashboard",
            "GET,  /api/moradores",
            "GET,  /api/moradores/ranking",
            "GET,  /api/espacos",
            "GET,  /api/agendamentos",
            "POST, /api/agendamentos",
            "GET,  /api/despesas",
            "POST, /api/despesas",
            "GET,  /api/tarefas",
            "GET,  /api/tarefas-recorrentes",
            "GET,  /api/leituras-consumo",
            "POST, /api/leituras-consumo",
            "GET,  /api/regras-penalidade",
    })
    void dadosFechados(String metodo, String url) throws Exception {
        assertEquals(403, chamar(metodo, url, SEM_CASA, SENHA));
        assertEquals(403, chamar(metodo, url, PENDENTE, SENHA));
    }

    @Test
    @DisplayName("A mensagem diz o que fazer: criar/entrar, ou aguardar aprovação")
    void mensagemOrienta() throws Exception {
        mvc.perform(get("/api/dashboard").with(httpBasic(SEM_CASA, SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Sem Casa Ativa"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Crie uma casa")));
        mvc.perform(get("/api/dashboard").with(httpBasic(PENDENTE, SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("aprovação")));
    }

    @Test
    @DisplayName("Conta, casa e perfil continuam acessíveis a quem ainda não tem casa ativa")
    void rotasDeContaAbertas() throws Exception {
        for (String email : new String[]{SEM_CASA, PENDENTE}) {
            mvc.perform(get("/api/auth/eu").with(httpBasic(email, SENHA))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/casas/minha").with(httpBasic(PENDENTE, SENHA))).andExpect(status().isOk());
        mvc.perform(get("/api/casas/minha").with(httpBasic(SEM_CASA, SENHA))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Depois de aprovado, o morador passa pela porta")
    void aprovadoEntra() throws Exception {
        assertEquals(403, chamar("GET", "/api/espacos", PENDENTE, SENHA));

        String json = mvc.perform(get("/api/casas/pendentes")
                        .with(httpBasic("helena.vance@homelive2050.io", "homelive123")))
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(json, "$[0].id");
        mvc.perform(post("/api/casas/membros/" + id + "/aprovar")
                        .with(httpBasic("helena.vance@homelive2050.io", "homelive123")))
                .andExpect(status().isOk());

        assertEquals(200, chamar("GET", "/api/espacos", PENDENTE, SENHA));
    }

    @Test
    @DisplayName("Quem fundou a casa (síndico novo) já entra direto")
    void fundadorEntra() throws Exception {
        mvc.perform(post("/api/casas").with(httpBasic(SEM_CASA, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Minha\"}"))
                .andExpect(status().isCreated());
        assertEquals(200, chamar("GET", "/api/espacos", SEM_CASA, SENHA));
    }

    @Test
    @DisplayName("Membro ativo da casa de demonstração segue normal")
    void ativoNormal() throws Exception {
        assertEquals(200, chamar("GET", "/api/dashboard", ATIVO, "homelive123"));
    }
}
