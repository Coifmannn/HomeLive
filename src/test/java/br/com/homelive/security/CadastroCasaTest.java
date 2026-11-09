package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import br.com.homelive.repository.CasaRepository;
import br.com.homelive.repository.EspacoComumRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import br.com.homelive.repository.UsuarioRepository;
import br.com.homelive.service.CasaService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Cadastro público: criar a residência ou entrar com o código de convite")
class CadastroCasaTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CasaRepository casaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EspacoComumRepository espacoRepository;
    @Autowired
    private RegraPenalidadeRepository regraRepository;
    @Autowired
    private TarefaRecorrenteRepository escalaRepository;
    @Autowired
    private CasaService casaService;

    // ---- utilitários -----------------------------------------------------------------

    private ResultActions cadastrar(String json) throws Exception {
        return mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String codigoAtual() {
        return casaRepository.findFirstByOrderByIdAsc().orElseThrow().getCodigoConvite();
    }

    private String jsonMorador(String email, String codigo) {
        return "{\"nome\":\"Nova Pessoa\",\"email\":\"" + email + "\",\"senha\":\"senha123\","
                + "\"numeroQuarto\":\"Suíte 305\",\"codigoConvite\":\"" + codigo + "\"}";
    }

    private String jsonCriarCasa(String email, String nomeCasa, String extra) {
        return "{\"nome\":\"Fundadora\",\"email\":\"" + email + "\",\"senha\":\"senha123\","
                + "\"nomeCasa\":\"" + nomeCasa + "\"" + extra + "}";
    }

    /** Simula o banco sem residência (primeiro uso). Desfeito ao fim do teste (rollback). */
    private void esvaziarCasas() {
        casaRepository.deleteAll();
        casaRepository.flush();
    }

    // ---- estado público --------------------------------------------------------------

    @Test
    @DisplayName("Estado da residência é público e diz se já existe")
    void estadoPublico() throws Exception {
        mvc.perform(get("/api/casa/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casaCriada").value(true))
                .andExpect(jsonPath("$.nomeCasa").value("Residência HomeLive 2050"));

        esvaziarCasas();

        mvc.perform(get("/api/casa/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casaCriada").value(false));
    }

    // ---- entrar como morador ---------------------------------------------------------

    @Test
    @DisplayName("Com o código certo a pessoa vira MORADOR e já sai logada")
    void entrarComCodigo() throws Exception {
        String resposta = cadastrar(jsonMorador("nova.pessoa@homelive2050.io", codigoAtual()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.papel").value("MORADOR"))
                .andExpect(jsonPath("$.usuario.numeroQuarto").value("Suíte 305"))
                .andExpect(jsonPath("$.usuario.pontosConvivencia").value(100))
                .andReturn().getResponse().getContentAsString();

        // o token recém-entregue já abre a API
        String token = JsonPath.read(resposta, "$.token");
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        // e a senha escolhida funciona no login normal
        mvc.perform(get("/api/espacos").with(httpBasic("nova.pessoa@homelive2050.io", "senha123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("O código vale em minúsculas e com espaços")
    void codigoSemRigor() throws Exception {
        cadastrar(jsonMorador("minuscula@homelive2050.io", "  " + codigoAtual().toLowerCase() + " "))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Código errado: 400 e nenhuma conta é criada")
    void codigoErrado() throws Exception {
        cadastrar(jsonMorador("intruso@homelive2050.io", "HL-ERRADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Código de convite inválido."));
        assertFalse(usuarioRepository.existsByEmail("intruso@homelive2050.io"));
    }

    @Test
    @DisplayName("Já existe residência: quem tenta criar outra, sem código, é barrado (não vira síndico)")
    void naoDaParaSequestrarACasa() throws Exception {
        cadastrar(jsonCriarCasa("golpista@homelive2050.io", "Minha casa", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("código de convite")));
        assertFalse(usuarioRepository.existsByEmail("golpista@homelive2050.io"));
    }

    @Test
    @DisplayName("Sem o quarto, o morador não é cadastrado")
    void quartoObrigatorio() throws Exception {
        cadastrar("{\"nome\":\"Sem Quarto\",\"email\":\"semquarto@homelive2050.io\",\"senha\":\"senha123\","
                + "\"codigoConvite\":\"" + codigoAtual() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe o seu quarto."));
    }

    @Test
    @DisplayName("E-mail já usado (até com outra caixa): 400")
    void emailDuplicado() throws Exception {
        cadastrar(jsonMorador(MORADOR, codigoAtual())).andExpect(status().isBadRequest());
        cadastrar(jsonMorador(MORADOR.toUpperCase(), codigoAtual())).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dados inválidos (e-mail, senha curta, nome vazio): 400")
    void validacao() throws Exception {
        cadastrar("{\"nome\":\"X\",\"email\":\"nao-e-email\",\"senha\":\"senha123\"}").andExpect(status().isBadRequest());
        cadastrar("{\"nome\":\"X\",\"email\":\"a@b.io\",\"senha\":\"123\"}").andExpect(status().isBadRequest());
        cadastrar("{\"nome\":\"\",\"email\":\"a@b.io\",\"senha\":\"senha123\"}").andExpect(status().isBadRequest());
    }

    // ---- criar a residência ----------------------------------------------------------

    @Test
    @DisplayName("Sem residência, o primeiro cadastro CRIA a casa e a pessoa vira SÍNDICO")
    void criarCasa() throws Exception {
        esvaziarCasas();

        String resposta = cadastrar(jsonCriarCasa("fundadora@homelive2050.io", "Casa Aurora", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("SINDICO"))
                .andReturn().getResponse().getContentAsString();

        assertEquals("Casa Aurora", casaRepository.findFirstByOrderByIdAsc().orElseThrow().getNome());

        // o síndico novo enxerga o código de convite da casa que acabou de criar
        String token = JsonPath.read(resposta, "$.token");
        mvc.perform(get("/api/casa").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Casa Aurora"))
                .andExpect(jsonPath("$.codigoConvite", not(emptyString())));
    }

    @Test
    @DisplayName("Criar a casa sem dar nome a ela: 400")
    void criarCasaExigeNome() throws Exception {
        esvaziarCasas();

        cadastrar("{\"nome\":\"Fundadora\",\"email\":\"semnome@homelive2050.io\",\"senha\":\"senha123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nome da residência")));
        assertFalse(usuarioRepository.existsByEmail("semnome@homelive2050.io"));
    }

    @Test
    @DisplayName("Por padrão a nova casa já nasce com espaços, regras e escalas sugeridos")
    void criarCasaComSugestoes() throws Exception {
        esvaziarCasas();
        long espacos = espacoRepository.count();
        long regras = regraRepository.count();
        long escalas = escalaRepository.count();

        cadastrar(jsonCriarCasa("sugestoes@homelive2050.io", "Casa Sugestões", "")).andExpect(status().isCreated());

        assertEquals(espacos + 3, espacoRepository.count());
        assertEquals(regras + 3, regraRepository.count());
        assertEquals(escalas + 3, escalaRepository.count());
    }

    @Test
    @DisplayName("Desmarcando as sugestões, a casa nasce vazia")
    void criarCasaSemSugestoes() throws Exception {
        esvaziarCasas();
        long espacos = espacoRepository.count();
        long regras = regraRepository.count();
        long escalas = escalaRepository.count();

        cadastrar(jsonCriarCasa("vazia@homelive2050.io", "Casa Vazia", ",\"usarSugestoes\":false"))
                .andExpect(status().isCreated());

        assertEquals(espacos, espacoRepository.count());
        assertEquals(regras, regraRepository.count());
        assertEquals(escalas, escalaRepository.count());
    }

    // ---- dados da casa e convite -----------------------------------------------------

    @Test
    @DisplayName("Síndico vê o código de convite; o morador vê a casa, mas não o código")
    void codigoSoParaSindico() throws Exception {
        mvc.perform(get("/api/casa").with(httpBasic(SINDICO, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoConvite").value(codigoAtual()));

        mvc.perform(get("/api/casa").with(httpBasic(MORADOR, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Residência HomeLive 2050"))
                .andExpect(jsonPath("$.codigoConvite").doesNotExist());
    }

    @Test
    @DisplayName("Sem login, os dados da casa são negados")
    void dadosDaCasaExigemLogin() throws Exception {
        mvc.perform(get("/api/casa")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Renovar o convite (síndico): o código antigo para de valer e o novo vale")
    void renovarConvite() throws Exception {
        String antigo = codigoAtual();

        mvc.perform(post("/api/casa/convite/renovar").with(httpBasic(SINDICO, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoConvite", not(emptyString())));

        String novo = codigoAtual();
        assertNotEquals(antigo, novo);

        cadastrar(jsonMorador("velho@homelive2050.io", antigo)).andExpect(status().isBadRequest());
        cadastrar(jsonMorador("novo@homelive2050.io", novo)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Morador não renova o convite: 403")
    void moradorNaoRenova() throws Exception {
        String antes = codigoAtual();
        mvc.perform(post("/api/casa/convite/renovar").with(httpBasic(MORADOR, DataInitializer.SENHA_DEMO)))
                .andExpect(status().isForbidden());
        assertEquals(antes, codigoAtual());
    }

    // ---- banco antigo (sem casa, mas com usuários) -----------------------------------

    @Test
    @DisplayName("Banco que já tinha usuários e nenhuma casa ganha uma casa, para ninguém virar síndico à força")
    void bancoAntigoGanhaCasa() throws Exception {
        esvaziarCasas();
        assertTrue(usuarioRepository.count() > 0);

        casaService.garantirCasaSeHouverUsuarios();

        assertTrue(casaRepository.findFirstByOrderByIdAsc().isPresent());
        mvc.perform(get("/api/casa/estado")).andExpect(jsonPath("$.casaCriada").value(true));
        cadastrar(jsonCriarCasa("invasor@homelive2050.io", "Outra", "")).andExpect(status().isBadRequest());
    }
}
