package br.com.homelive.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo completo: cadastrar -> criar casa (vira síndico) / entrar com código -> aprovar ou recusar. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Fluxo de cadastro, criação e entrada na casa")
class CasaFluxoIntegracaoTest {

    private static final String SENHA = "segredo123";
    private static final String DONO = "dono@teste.io";
    private static final String NOVATO = "novato@teste.io";
    private static final String CODIGO_DEMO = "HL-DEMO";

    @Autowired
    private MockMvc mvc;

    private ResultActions cadastrar(String email) throws Exception {
        return mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Fulano\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"));
    }

    private ResultActions criarCasa(String email, String nome) throws Exception {
        return mvc.perform(post("/api/casas").with(httpBasic(email, SENHA))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"" + nome + "\"}"));
    }

    private ResultActions entrar(String email, String codigo, String quarto) throws Exception {
        return mvc.perform(post("/api/casas/entrar").with(httpBasic(email, SENHA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoConvite\":\"" + codigo + "\",\"numeroQuarto\":\"" + quarto + "\"}"));
    }

    private ResultActions eu(String email) throws Exception {
        return mvc.perform(get("/api/auth/eu").with(httpBasic(email, SENHA)));
    }

    /** Cadastra o dono, funda uma casa e devolve o código de convite. */
    private String fundarCasa() throws Exception {
        cadastrar(DONO).andExpect(status().isCreated());
        String json = criarCasa(DONO, "Casa Teste").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.codigoConvite");
    }

    // ---- cadastro ----

    @Test
    @DisplayName("Cadastro é público, cria só a conta e não devolve a senha")
    void cadastroPublico() throws Exception {
        cadastrar(NOVATO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(NOVATO))
                .andExpect(jsonPath("$.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.statusMembro").value("SEM_CASA"))
                .andExpect(jsonPath("$.casaId").value(nullValue()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        eu(NOVATO).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cadastro recusa e-mail repetido e dados inválidos com 400")
    void cadastroInvalido() throws Exception {
        cadastrar(NOVATO).andExpect(status().isCreated());
        cadastrar(NOVATO).andExpect(status().isBadRequest());
        cadastrar("helena.vance@homelive2050.io").andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"nao-e-email\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- criar casa ----

    @Test
    @DisplayName("Quem funda a casa vira síndico, com as mesmas credenciais")
    void fundarViraSindico() throws Exception {
        cadastrar(DONO).andExpect(status().isCreated());
        criarCasa(DONO, "Casa Teste").andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Casa Teste"))
                .andExpect(jsonPath("$.codigoConvite").value(matchesPattern("HL-[A-Z2-9]{4}")));

        eu(DONO).andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SINDICO"))
                .andExpect(jsonPath("$.statusMembro").value("ATIVO"))
                .andExpect(jsonPath("$.casaNome").value("Casa Teste"));
    }

    @Test
    @DisplayName("Quem já tem casa não funda outra")
    void naoFundaDuasCasas() throws Exception {
        fundarCasa();
        criarCasa(DONO, "Segunda").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nome da casa é obrigatório")
    void nomeObrigatorio() throws Exception {
        cadastrar(DONO).andExpect(status().isCreated());
        criarCasa(DONO, "").andExpect(status().isBadRequest());
    }

    // ---- entrar ----

    @Test
    @DisplayName("Entrar com o código deixa PENDENTE e sem revelar o código de convite")
    void entrarFicaPendente() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());

        entrar(NOVATO, codigo.toLowerCase(), "Suíte 7").andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Casa Teste"))
                .andExpect(jsonPath("$.codigoConvite").value(nullValue()));

        eu(NOVATO).andExpect(jsonPath("$.statusMembro").value("PENDENTE"))
                .andExpect(jsonPath("$.tipo").value("MORADOR"));
        mvc.perform(get("/api/casas/minha").with(httpBasic(NOVATO, SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoConvite").value(nullValue()));
    }

    @Test
    @DisplayName("Código inexistente: 404; sem quarto: 400; já com casa: 400")
    void entrarComProblemas() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());

        entrar(NOVATO, "HL-NAOEXISTE", "1").andExpect(status().isNotFound());
        entrar(NOVATO, codigo, "").andExpect(status().isBadRequest());
        entrar(NOVATO, codigo, "1").andExpect(status().isOk());
        entrar(NOVATO, CODIGO_DEMO, "2").andExpect(status().isBadRequest());
        entrar(DONO, CODIGO_DEMO, "2").andExpect(status().isBadRequest()); // síndico já tem casa
    }

    @Test
    @DisplayName("Sem casa, GET /api/casas/minha dá 404; o síndico vê o próprio código")
    void minhaCasa() throws Exception {
        cadastrar(NOVATO).andExpect(status().isCreated());
        mvc.perform(get("/api/casas/minha").with(httpBasic(NOVATO, SENHA))).andExpect(status().isNotFound());

        String codigo = fundarCasa();
        mvc.perform(get("/api/casas/minha").with(httpBasic(DONO, SENHA)))
                .andExpect(jsonPath("$.codigoConvite").value(codigo));
    }

    // ---- aprovação ----

    @Test
    @DisplayName("Síndico lista os pendentes e aprova: o morador vira ATIVO")
    void aprovar() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());
        entrar(NOVATO, codigo, "Suíte 7").andExpect(status().isOk());

        String json = mvc.perform(get("/api/casas/pendentes").with(httpBasic(DONO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(NOVATO))
                .andExpect(jsonPath("$[0].numeroQuarto").value("Suíte 7"))
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(json, "$[0].id");

        mvc.perform(post("/api/casas/membros/" + id + "/aprovar").with(httpBasic(DONO, SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusMembro").value("ATIVO"));

        eu(NOVATO).andExpect(jsonPath("$.statusMembro").value("ATIVO"));
        mvc.perform(get("/api/casas/pendentes").with(httpBasic(DONO, SENHA))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/casas/membros/" + id + "/aprovar").with(httpBasic(DONO, SENHA)))
                .andExpect(status().isBadRequest()); // já aprovado
    }

    @Test
    @DisplayName("Recusar devolve a pessoa a SEM_CASA e ela pode tentar outro código")
    void recusar() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());
        entrar(NOVATO, codigo, "Suíte 7").andExpect(status().isOk());
        int id = idDoPendente();

        mvc.perform(post("/api/casas/membros/" + id + "/recusar").with(httpBasic(DONO, SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusMembro").value("SEM_CASA"));
        eu(NOVATO).andExpect(jsonPath("$.statusMembro").value("SEM_CASA")).andExpect(jsonPath("$.casaId").value(nullValue()));

        entrar(NOVATO, CODIGO_DEMO, "Suíte 9").andExpect(status().isOk());
        eu(NOVATO).andExpect(jsonPath("$.casaNome").value("HomeLive 2050"));
    }

    @Test
    @DisplayName("Síndico de outra casa não enxerga nem decide sobre pendentes alheios (404)")
    void isolamentoEntreCasas() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());
        entrar(NOVATO, codigo, "Suíte 7").andExpect(status().isOk());
        int id = idDoPendente();

        // Helena é síndica da casa de demonstração, não da casa do DONO.
        mvc.perform(get("/api/casas/pendentes").with(httpBasic("helena.vance@homelive2050.io", "homelive123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/casas/membros/" + id + "/aprovar").with(httpBasic("helena.vance@homelive2050.io", "homelive123")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/casas/membros/" + id + "/recusar").with(httpBasic("helena.vance@homelive2050.io", "homelive123")))
                .andExpect(status().isNotFound());
        eu(NOVATO).andExpect(jsonPath("$.statusMembro").value("PENDENTE"));
    }

    @Test
    @DisplayName("Morador comum não aprova ninguém (403)")
    void moradorNaoAprova() throws Exception {
        String codigo = fundarCasa();
        cadastrar(NOVATO).andExpect(status().isCreated());
        entrar(NOVATO, codigo, "Suíte 7").andExpect(status().isOk());
        int id = idDoPendente();

        mvc.perform(post("/api/casas/membros/" + id + "/aprovar").with(httpBasic(NOVATO, SENHA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/casas/pendentes").with(httpBasic(NOVATO, SENHA))).andExpect(status().isForbidden());
    }

    private int idDoPendente() throws Exception {
        String json = mvc.perform(get("/api/casas/pendentes").with(httpBasic(DONO, SENHA)))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$[0].id");
    }
}
