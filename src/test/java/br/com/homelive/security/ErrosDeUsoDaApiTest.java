package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Erros de uso da API respondem com o status certo (e não 500)")
class ErrosDeUsoDaApiTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";

    @Autowired
    private MockMvc mvc;

    private MockHttpServletRequestBuilder logado(MockHttpServletRequestBuilder req) {
        return req.with(httpBasic(SINDICO, DataInitializer.SENHA_DEMO));
    }

    @Test
    @DisplayName("Rota inexistente: 404")
    void rotaInexistente() throws Exception {
        mvc.perform(logado(get("/api/nao-existe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Rota Não Encontrada"));
    }

    @Test
    @DisplayName("Método errado para a rota: 405")
    void metodoNaoPermitido() throws Exception {
        mvc.perform(logado(put("/api/dashboard"))).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("JSON malformado ou ausente: 400")
    void jsonMalformado() throws Exception {
        mvc.perform(logado(post("/api/despesas")).contentType(MediaType.APPLICATION_JSON).content("{ isso não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Corpo da Requisição Inválido"));
        mvc.perform(logado(post("/api/despesas")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Parâmetro obrigatório ausente: 400")
    void parametroAusente() throws Exception {
        mvc.perform(logado(patch("/api/moradores/2/estadia")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta o parâmetro 'dias'."));
    }

    @Test
    @DisplayName("Id ou valor de tipo errado na URL: 400")
    void tipoInvalido() throws Exception {
        mvc.perform(logado(get("/api/moradores/abc"))).andExpect(status().isBadRequest());
        mvc.perform(logado(get("/api/despesas").param("status", "INEXISTENTE"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Recurso inexistente com id válido continua sendo 404")
    void recursoInexistente() throws Exception {
        mvc.perform(logado(get("/api/moradores/99999"))).andExpect(status().isNotFound());
    }
}
