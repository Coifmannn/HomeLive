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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Ninguém cadastra morador ou síndico "por fora": a pessoa se cadastra sozinha e entra numa casa
 * (ou funda uma). Estas rotas não existem mais, nem para o síndico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Rotas de cadastro avulso foram removidas")
class RotasRemovidasTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{0} {1}: síndico não encontra a rota")
    @DisplayName("Síndico recebe 404/405, nunca sucesso")
    @CsvSource({
            "POST, /api/moradores",
            "POST, /api/sindicos",
            "GET,  /api/sindicos",
            "GET,  /api/sindicos/1",
    })
    void rotaNaoExiste(String metodo, String url) throws Exception {
        int status = mvc.perform(request(HttpMethod.valueOf(metodo), url)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(httpBasic("helena.vance@homelive2050.io", DataInitializer.SENHA_DEMO)))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 404 || status == 405, "esperava 404/405, veio " + status);
    }
}
