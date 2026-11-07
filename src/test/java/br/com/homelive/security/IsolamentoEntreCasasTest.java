package br.com.homelive.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O coração do multi-casa: a casa A é a de demonstração (síndica Helena); a casa B é criada aqui, com um
 * síndico e uma moradora. Nada de uma casa pode ser visto, lido por id ou alterado pela outra.
 * Regra: o que é de outra casa responde 404, como se não existisse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Isolamento entre casas")
class IsolamentoEntreCasasTest {

    private static final String SENHA_A = "homelive123";
    private static final String SENHA_B = "segredo123";
    private static final String HELENA = "helena.vance@homelive2050.io"; // síndica da casa A
    private static final String LUCAS = "lucas.silva@homelive2050.io";   // moradora/morador da casa A
    private static final String SINDICO_B = "sindico.b@teste.io";
    private static final String BIA = "bia.b@teste.io";                  // moradora da casa B

    @Autowired
    private MockMvc mvc;

    private long idBia;

    // ---------------------------------------------------------------- montagem

    @BeforeEach
    void montarCasaB() throws Exception {
        cadastrar(SINDICO_B);
        String codigo = JsonPath.read(corpo(chamar(HttpMethod.POST, "/api/casas", SINDICO_B, SENHA_B,
                "{\"nome\":\"Casa B\"}").andExpect(status().isCreated())), "$.codigoConvite");

        cadastrar(BIA);
        chamar(HttpMethod.POST, "/api/casas/entrar", BIA, SENHA_B,
                "{\"codigoConvite\":\"" + codigo + "\",\"numeroQuarto\":\"B-1\"}").andExpect(status().isOk());
        idBia = ((Number) JsonPath.read(corpo(chamar(HttpMethod.GET, "/api/casas/pendentes", SINDICO_B, SENHA_B, null)),
                "$[0].id")).longValue();
        chamar(HttpMethod.POST, "/api/casas/membros/" + idBia + "/aprovar", SINDICO_B, SENHA_B, null)
                .andExpect(status().isOk());
    }

    private void cadastrar(String email) throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA_B + "\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions chamar(HttpMethod metodo, String url, String usuario, String senha, String corpo) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, url).with(httpBasic(usuario, senha));
        if (corpo != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(req);
    }

    private ResultActions comoA(HttpMethod metodo, String url, String corpo) throws Exception {
        return chamar(metodo, url, HELENA, SENHA_A, corpo);
    }

    private ResultActions comoB(HttpMethod metodo, String url, String corpo) throws Exception {
        return chamar(metodo, url, SINDICO_B, SENHA_B, corpo);
    }

    private String corpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    /** Id do primeiro item de uma lista da casa A (ex.: "/api/espacos"). */
    private long primeiroDeA(String url) throws Exception {
        return ((Number) JsonPath.read(corpo(comoA(HttpMethod.GET, url, null).andExpect(status().isOk())), "$[0].id")).longValue();
    }

    private long idDe(String email) throws Exception {
        String json = corpo(comoA(HttpMethod.GET, "/api/moradores", null));
        java.util.List<Number> ids = JsonPath.read(json, "$[?(@.email=='" + email + "')].id");
        return ids.get(0).longValue();
    }

    private void esperar404(HttpMethod metodo, String url, String corpo) throws Exception {
        comoB(metodo, url, corpo).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- listas

    @Test
    @DisplayName("A casa B nasce vazia: nenhuma lista traz dados da casa A")
    void listasDaCasaBNaoTrazDadosDaCasaA() throws Exception {
        for (String url : new String[]{"/api/espacos", "/api/despesas", "/api/tarefas", "/api/tarefas-recorrentes",
                "/api/agendamentos", "/api/penalidades", "/api/leituras-consumo"}) {
            comoB(HttpMethod.GET, url, null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
        // Só a moradora aprovada de B: nem a Helena, nem o Lucas, nem o próprio síndico B.
        comoB(HttpMethod.GET, "/api/moradores", null).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(BIA));
        comoB(HttpMethod.GET, "/api/moradores/ranking", null).andExpect(jsonPath("$", hasSize(1)));
        comoB(HttpMethod.GET, "/api/moradores?apenasAtivos=true", null).andExpect(jsonPath("$", hasSize(1)));
        comoB(HttpMethod.GET, "/api/espacos?apenasAtivos=true", null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Dashboard da casa B só conta a casa B")
    void dashboardIsolado() throws Exception {
        comoB(HttpMethod.GET, "/api/dashboard", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMoradores").value(1))
                .andExpect(jsonPath("$.totalDespesasMes").value(0))
                .andExpect(jsonPath("$.totalTarefasPendentes").value(0))
                .andExpect(jsonPath("$.totalAgendamentosHoje").value(0))
                .andExpect(jsonPath("$.rankingMoradores", hasSize(1)))
                .andExpect(jsonPath("$.proximosAgendamentos", hasSize(0)));
        // E a casa A continua com os 4 moradores do seed.
        comoA(HttpMethod.GET, "/api/dashboard", null).andExpect(jsonPath("$.totalMoradores").value(4));
    }

    @Test
    @DisplayName("Cada casa nasce com as suas próprias regras de penalidade padrão")
    void regrasPadraoPorCasa() throws Exception {
        comoB(HttpMethod.GET, "/api/regras-penalidade", null).andExpect(jsonPath("$", hasSize(3)));
        long regraDeA = primeiroDeA("/api/regras-penalidade");
        long regraDeB = ((Number) JsonPath.read(corpo(comoB(HttpMethod.GET, "/api/regras-penalidade", null)), "$[0].id")).longValue();
        assertNotEquals(regraDeA, regraDeB);

        // Desativar a regra de B não mexe na de A.
        comoB(HttpMethod.PATCH, "/api/regras-penalidade/" + regraDeB + "/desativar", null).andExpect(status().isOk());
        comoA(HttpMethod.GET, "/api/regras-penalidade/" + regraDeA, null).andExpect(jsonPath("$.ativa").value(true));
    }

    // ---------------------------------------------------------------- leitura por id

    @Test
    @DisplayName("Ler por id um recurso da casa A, estando na casa B: 404 em todos os módulos")
    void lerPorIdDeOutraCasa() throws Exception {
        long espaco = primeiroDeA("/api/espacos");
        long despesa = primeiroDeA("/api/despesas");
        long tarefa = primeiroDeA("/api/tarefas");
        long recorrente = primeiroDeA("/api/tarefas-recorrentes");
        long regra = primeiroDeA("/api/regras-penalidade");
        long lucas = idDe(LUCAS);

        esperar404(HttpMethod.GET, "/api/espacos/" + espaco, null);
        esperar404(HttpMethod.GET, "/api/despesas/" + despesa, null);
        esperar404(HttpMethod.GET, "/api/tarefas/" + tarefa, null);
        esperar404(HttpMethod.GET, "/api/tarefas-recorrentes/" + recorrente, null);
        esperar404(HttpMethod.GET, "/api/regras-penalidade/" + regra, null);
        esperar404(HttpMethod.GET, "/api/moradores/" + lucas, null);

        // Listas "por" alguém ou algo de outra casa também não vazam.
        esperar404(HttpMethod.GET, "/api/agendamentos/espaco/" + espaco, null);
        esperar404(HttpMethod.GET, "/api/agendamentos/morador/" + lucas, null);
        esperar404(HttpMethod.GET, "/api/despesas/morador/" + lucas + "/rateios", null);
        esperar404(HttpMethod.GET, "/api/tarefas/morador/" + lucas, null);
        esperar404(HttpMethod.GET, "/api/leituras-consumo/morador/" + lucas, null);
        esperar404(HttpMethod.GET, "/api/penalidades/morador/" + lucas, null);
    }

    // ---------------------------------------------------------------- escrita por id

    @Test
    @DisplayName("Alterar por id um recurso da casa A, estando na casa B: 404 e nada muda")
    void alterarPorIdDeOutraCasa() throws Exception {
        long espaco = primeiroDeA("/api/espacos");
        long despesa = primeiroDeA("/api/despesas");
        long tarefa = primeiroDeA("/api/tarefas");
        long recorrente = primeiroDeA("/api/tarefas-recorrentes");
        long regra = primeiroDeA("/api/regras-penalidade");
        long lucas = idDe(LUCAS);

        esperar404(HttpMethod.PATCH, "/api/espacos/" + espaco + "/status", null);
        esperar404(HttpMethod.POST, "/api/despesas/" + despesa + "/avaliar", "{\"aprovado\":false,\"motivoRejeicao\":\"x\"}");
        esperar404(HttpMethod.PATCH, "/api/tarefas/" + tarefa + "/concluir", null);
        esperar404(HttpMethod.PATCH, "/api/tarefas-recorrentes/" + recorrente + "/desativar", null);
        esperar404(HttpMethod.PATCH, "/api/tarefas-recorrentes/" + recorrente + "/ativar", null);
        esperar404(HttpMethod.PATCH, "/api/regras-penalidade/" + regra + "/desativar", null);
        esperar404(HttpMethod.PATCH, "/api/regras-penalidade/" + regra + "/ativar", null);
        esperar404(HttpMethod.PATCH, "/api/moradores/" + lucas + "/estadia?dias=1", null);
        esperar404(HttpMethod.DELETE, "/api/moradores/" + lucas, null);

        // Nada foi alterado na casa A.
        comoA(HttpMethod.GET, "/api/moradores/" + lucas, null).andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.diasEstadia").value(30));
        comoA(HttpMethod.GET, "/api/regras-penalidade/" + regra, null).andExpect(jsonPath("$.ativa").value(true));
        comoA(HttpMethod.GET, "/api/espacos/" + espaco, null).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("Reserva, cancelamento e não comparecimento entre casas: 404")
    void agendamentosEntreCasas() throws Exception {
        long espacoA = primeiroDeA("/api/espacos");
        long reservaA = primeiroDeA("/api/agendamentos");
        long lucas = idDe(LUCAS);

        // Espaço da casa B para provar que o problema é só o cruzamento.
        long espacoB = ((Number) JsonPath.read(corpo(comoB(HttpMethod.POST, "/api/espacos",
                "{\"nome\":\"Sala B\",\"capacidadeMaxima\":4}").andExpect(status().isCreated())), "$.id")).longValue();
        String quando = "\"dataHoraInicio\":\"" + LocalDateTime.now().plusDays(3).withNano(0)
                + "\",\"dataHoraFim\":\"" + LocalDateTime.now().plusDays(3).plusHours(1).withNano(0) + "\"";

        // Síndico B reservando o espaço de A, para a moradora de B.
        esperar404(HttpMethod.POST, "/api/agendamentos", "{\"espacoId\":" + espacoA + ",\"moradorId\":" + idBia + "," + quando + "}");
        // Síndico B reservando o espaço de B para um morador de A.
        esperar404(HttpMethod.POST, "/api/agendamentos", "{\"espacoId\":" + espacoB + ",\"moradorId\":" + lucas + "," + quando + "}");
        // E a reserva legítima dentro de B funciona.
        comoB(HttpMethod.POST, "/api/agendamentos", "{\"espacoId\":" + espacoB + ",\"moradorId\":" + idBia + "," + quando + "}")
                .andExpect(status().isCreated());

        esperar404(HttpMethod.PATCH, "/api/agendamentos/" + reservaA + "/cancelar", null);
        esperar404(HttpMethod.PATCH, "/api/agendamentos/" + reservaA + "/nao-compareceu", null);
        comoA(HttpMethod.GET, "/api/agendamentos", null).andExpect(jsonPath("$[0].status").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("Despesa, leitura, tarefa e penalidade apontando para morador de outra casa: 404")
    void moradorDeOutraCasaComoAlvo() throws Exception {
        long lucas = idDe(LUCAS);
        String hoje = LocalDate.now().toString();

        esperar404(HttpMethod.POST, "/api/leituras-consumo", "{\"kwh\":10,\"moradorId\":" + lucas + "}");
        esperar404(HttpMethod.POST, "/api/tarefas",
                "{\"titulo\":\"x\",\"dataPrazo\":\"" + LocalDate.now().plusDays(2) + "\",\"responsavelId\":" + lucas + "}");
        esperar404(HttpMethod.POST, "/api/penalidades", "{\"moradorId\":" + lucas + ",\"pontos\":5,\"motivo\":\"x\"}");

        // Com a moradora de B, as mesmas ações funcionam.
        comoB(HttpMethod.POST, "/api/leituras-consumo", "{\"kwh\":10,\"moradorId\":" + idBia + "}").andExpect(status().isOk());
        comoB(HttpMethod.POST, "/api/tarefas",
                "{\"titulo\":\"x\",\"dataPrazo\":\"" + LocalDate.now().plusDays(2) + "\",\"responsavelId\":" + idBia + "}")
                .andExpect(status().isCreated());
        comoB(HttpMethod.POST, "/api/penalidades", "{\"moradorId\":" + idBia + ",\"pontos\":5,\"motivo\":\"x\"}")
                .andExpect(status().isCreated());
        assertEquals(hoje, LocalDate.now().toString());
    }

    @Test
    @DisplayName("Item de rateio de outra casa não pode ser quitado")
    void quitarRateioDeOutraCasa() throws Exception {
        String despesas = corpo(comoA(HttpMethod.GET, "/api/despesas?status=RATEADA", null));
        long item = ((Number) JsonPath.read(despesas, "$[0].itensRateio[0].id")).longValue();

        esperar404(HttpMethod.PATCH, "/api/despesas/rateio/" + item + "/quitar", null);
        // Em A, quem tem o item (ou a síndica) quita normalmente.
        comoA(HttpMethod.PATCH, "/api/despesas/rateio/" + item + "/quitar", null).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- criação não vaza

    @Test
    @DisplayName("O que a casa B cria não aparece na casa A, e vice-versa")
    void criacaoNaoVaza() throws Exception {
        comoB(HttpMethod.POST, "/api/espacos", "{\"nome\":\"Sala B\",\"capacidadeMaxima\":4}").andExpect(status().isCreated());
        comoB(HttpMethod.POST, "/api/despesas",
                "{\"titulo\":\"Luz B\",\"valorTotal\":100,\"dataVencimento\":\"" + LocalDate.now().plusDays(5)
                        + "\",\"categoria\":\"ENERGIA_SOLAR\",\"tipoDespesa\":\"FIXA\",\"tipoRateio\":\"IGUALITARIO\"}")
                .andExpect(status().isCreated());

        comoB(HttpMethod.GET, "/api/espacos", null).andExpect(jsonPath("$", hasSize(1)));
        comoB(HttpMethod.GET, "/api/despesas", null).andExpect(jsonPath("$", hasSize(1)));
        comoA(HttpMethod.GET, "/api/espacos", null).andExpect(jsonPath("$", hasSize(3)));
        comoA(HttpMethod.GET, "/api/despesas", null).andExpect(jsonPath("$[?(@.titulo=='Luz B')]", hasSize(0)));
        comoA(HttpMethod.GET, "/api/dashboard", null).andExpect(jsonPath("$.totalMoradores").value(4));
    }

    // ---------------------------------------------------------------- rodízio e rotinas

    @Test
    @DisplayName("Gerar as tarefas da casa A não gera nada para a casa B (e vice-versa)")
    void rodizioPorCasa() throws Exception {
        String hoje = LocalDate.now().toString();
        comoB(HttpMethod.POST, "/api/tarefas-recorrentes",
                "{\"titulo\":\"Lixo B\",\"periodicidade\":\"SEMANAL\",\"prazoEmDias\":2,\"tipoRodizio\":\"SEQUENCIAL\","
                        + "\"primeiraGeracao\":\"" + hoje + "\"}").andExpect(status().isCreated());

        // A gera as dela: nenhuma tarefa de B aparece, e a recorrente de B continua devida.
        String geradasA = corpo(comoA(HttpMethod.POST, "/api/tarefas-recorrentes/gerar", null).andExpect(status().isOk()));
        assertEquals(0, ((java.util.List<?>) JsonPath.read(geradasA, "$.tarefas[?(@.titulo=='Lixo B')]")).size());
        comoB(HttpMethod.GET, "/api/tarefas", null).andExpect(jsonPath("$", hasSize(0)));

        // B gera as dela: só a moradora de B pode receber.
        comoB(HttpMethod.POST, "/api/tarefas-recorrentes/gerar", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGeradas").value(1))
                .andExpect(jsonPath("$.tarefas[0].responsavelId").value(idBia));
        comoA(HttpMethod.GET, "/api/tarefas", null).andExpect(jsonPath("$[?(@.titulo=='Lixo B')]", hasSize(0)));
    }

    @Test
    @DisplayName("Processar atrasos em B não penaliza a casa A")
    void atrasosPorCasa() throws Exception {
        // O seed de A tem uma tarefa vencida (do Mateus). Processar em B não pode tocá-la.
        comoB(HttpMethod.POST, "/api/tarefas/processar-atrasos", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.tarefasPenalizadas").value(0));
        comoA(HttpMethod.GET, "/api/tarefas?status=PENDENTE", null)
                .andExpect(jsonPath("$[?(@.titulo=='Triagem e Descarte de Metais Raros')]", hasSize(1)));
        comoA(HttpMethod.GET, "/api/penalidades", null).andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------------------------------------------------------------- permissões continuam valendo

    @Test
    @DisplayName("Morador de B continua sem poder de síndico, e não enxerga a casa A")
    void moradorDeBNaoEhSindico() throws Exception {
        chamar(HttpMethod.POST, "/api/espacos", BIA, SENHA_B, "{\"nome\":\"x\"}").andExpect(status().isForbidden());
        chamar(HttpMethod.GET, "/api/espacos", BIA, SENHA_B, null).andExpect(jsonPath("$", hasSize(0)));
    }
}
