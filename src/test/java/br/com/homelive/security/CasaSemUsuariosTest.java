package br.com.homelive.security;

import br.com.homelive.repository.CasaRepository;
import br.com.homelive.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Cadastro quando existe uma casa, mas nenhum usuário")
class CasaSemUsuariosTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CasaRepository casaRepository;
    @MockitoSpyBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("O código de convite não é exigido: ninguém poderia passá-lo, então o cadastro cria a casa")
    void naoExigeCodigo() throws Exception {
        Mockito.doReturn(0L).when(usuarioRepository).count();

        mvc.perform(get("/api/casa/estado"))
                .andExpect(jsonPath("$.casaCriada").value(false));

        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Primeira\",\"email\":\"primeira@homelive2050.io\",\"senha\":\"senha123\","
                                + "\"nomeCasa\":\"Casa Nova\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("SINDICO"));

        assertEquals(1, casaRepository.count());
        assertEquals("Casa Nova", casaRepository.findFirstByOrderByIdAsc().orElseThrow().getNome());
    }
}
