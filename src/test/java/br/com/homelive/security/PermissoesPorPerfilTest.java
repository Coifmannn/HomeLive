package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Matriz de permissões por perfil")
class PermissoesPorPerfilTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String MORADOR = "lucas.silva@homelive2050.io";

    @Autowired
    private MockMvc mvc;

    private int chamar(HttpMethod metodo, String url, String usuario) throws Exception {
        var req = request(metodo, url).contentType(MediaType.APPLICATION_JSON).content("{}");
        if (usuario != null) {
            req = req.with(httpBasic(usuario, DataInitializer.SENHA_DEMO));
        }
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1}: morador leva 403")
    @DisplayName("Ações exclusivas do síndico são negadas ao morador")
    @CsvSource({
            "POST,   /api/moradores",
            "POST,   /api/sindicos",
            "POST,   /api/espacos",
            "POST,   /api/tarefas",
            "POST,   /api/tarefas/processar-atrasos",
            "POST,   /api/despesas/1/avaliar",
            "PATCH,  /api/espacos/1/status",
            "PATCH,  /api/moradores/2/estadia",
            "DELETE, /api/moradores/2",
    })
    void moradorNaoPodeAcoesDeSindico(String metodo, String url) throws Exception {
        assertEquals(403, chamar(HttpMethod.valueOf(metodo), url, MORADOR));
    }

    @ParameterizedTest(name = "{0} {1}: síndico passa pela segurança")
    @DisplayName("O síndico não é barrado pela segurança nessas ações")
    @CsvSource({
            "POST,   /api/moradores",
            "POST,   /api/sindicos",
            "POST,   /api/espacos",
            "POST,   /api/tarefas",
            "POST,   /api/tarefas/processar-atrasos",
            "POST,   /api/despesas/1/avaliar",
            "PATCH,  /api/espacos/1/status",
            "PATCH,  /api/moradores/2/estadia",
            "DELETE, /api/moradores/2",
    })
    void sindicoPassaPelaSeguranca(String metodo, String url) throws Exception {
        int status = chamar(HttpMethod.valueOf(metodo), url, SINDICO);
        assertNotEquals(401, status);
        assertNotEquals(403, status);
    }

    @ParameterizedTest(name = "{0} {1}: sem login leva 401")
    @DisplayName("Sem login, tudo exige autenticação")
    @CsvSource({
            "GET,    /api/dashboard",
            "GET,    /api/moradores/ranking",
            "POST,   /api/despesas",
            "POST,   /api/agendamentos",
            "PATCH,  /api/tarefas/1/concluir",
    })
    void semLoginNada(String metodo, String url) throws Exception {
        assertEquals(401, chamar(HttpMethod.valueOf(metodo), url, null));
    }

    @ParameterizedTest(name = "GET {0}: qualquer logado consulta")
    @DisplayName("Consultas ficam abertas a qualquer perfil autenticado")
    @CsvSource({
            "/api/dashboard", "/api/moradores", "/api/moradores/ranking", "/api/espacos",
            "/api/agendamentos", "/api/despesas", "/api/tarefas", "/api/sindicos",
    })
    void consultasParaQualquerLogado(String url) throws Exception {
        assertEquals(200, chamar(HttpMethod.GET, url, MORADOR));
        assertEquals(200, chamar(HttpMethod.GET, url, SINDICO));
    }
}
