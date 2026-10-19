package br.com.homelive.security;

import br.com.homelive.config.DataInitializer;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.Despesa;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.enums.StatusDespesa;
import br.com.homelive.model.enums.StatusPagamento;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import br.com.homelive.repository.DespesaRepository;
import br.com.homelive.repository.EspacoComumRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.RateioItemRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Regras de dono e identidade vinda do login")
class RegrasDeDonoTest {

    private static final String SINDICO = "helena.vance@homelive2050.io";
    private static final String LUCAS = "lucas.silva@homelive2050.io";
    private static final String SOFIA = "sofia.chen@homelive2050.io";

    @Autowired MockMvc mvc;
    @Autowired MoradorRepository moradorRepository;
    @Autowired AgendamentoEspacoRepository agendamentoRepository;
    @Autowired RateioItemRepository rateioItemRepository;
    @Autowired TarefaDomesticaRepository tarefaRepository;
    @Autowired DespesaRepository despesaRepository;
    @Autowired EspacoComumRepository espacoRepository;

    private Morador lucas;
    private Morador sofia;

    @BeforeEach
    void setUp() {
        lucas = moradorRepository.findAll().stream().filter(m -> m.getEmail().equals(LUCAS)).findFirst().orElseThrow();
        sofia = moradorRepository.findAll().stream().filter(m -> m.getEmail().equals(SOFIA)).findFirst().orElseThrow();
    }

    private MockHttpServletRequestBuilder como(String email, MockHttpServletRequestBuilder req) {
        return req.with(httpBasic(email, DataInitializer.SENHA_DEMO));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String corpo) {
        return req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    // ---------- Reservas ----------

    @Test
    @DisplayName("Morador cancela a própria reserva, mas não a de outro; síndico cancela qualquer uma")
    void cancelarReserva() throws Exception {
        AgendamentoEspaco doLucas = agendamentoRepository.findByMoradorId(lucas.getId()).get(0);
        AgendamentoEspaco daSofia = agendamentoRepository.findByMoradorId(sofia.getId()).get(0);

        mvc.perform(como(SOFIA, patch("/api/agendamentos/{id}/cancelar", doLucas.getId()))).andExpect(status().isForbidden());
        mvc.perform(como(LUCAS, patch("/api/agendamentos/{id}/cancelar", doLucas.getId()))).andExpect(status().isNoContent());
        mvc.perform(como(SINDICO, patch("/api/agendamentos/{id}/cancelar", daSofia.getId()))).andExpect(status().isNoContent());
    }

    private String reserva(Long espacoId, Long moradorId) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return "{\"espacoId\":" + espacoId
                + (moradorId != null ? ",\"moradorId\":" + moradorId : "")
                + ",\"dataHoraInicio\":\"" + inicio + "\",\"dataHoraFim\":\"" + inicio.plusHours(1) + "\"}";
    }

    @Test
    @DisplayName("Morador reserva sempre em nome próprio e não consegue reservar por outro")
    void reservaDoMorador() throws Exception {
        EspacoComum espaco = espacoRepository.findAll().get(2);

        mvc.perform(json(como(LUCAS, post("/api/agendamentos")), reserva(espaco.getId(), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moradorNome").value("Lucas Silva"));

        mvc.perform(json(como(LUCAS, post("/api/agendamentos")), reserva(espaco.getId(), sofia.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Síndico reserva em nome de um morador, e precisa informar qual")
    void reservaDoSindico() throws Exception {
        EspacoComum espaco = espacoRepository.findAll().get(2);

        mvc.perform(json(como(SINDICO, post("/api/agendamentos")), reserva(espaco.getId(), null)))
                .andExpect(status().isBadRequest());

        mvc.perform(json(como(SINDICO, post("/api/agendamentos")), reserva(espaco.getId(), sofia.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moradorNome").value("Sofia Chen"));
    }

    // ---------- Rateio ----------

    @Test
    @DisplayName("Morador quita só o próprio rateio; síndico quita qualquer um")
    void quitarRateio() throws Exception {
        RateioItem doLucas = rateioItemRepository.findByMoradorIdAndStatusPagamento(lucas.getId(), StatusPagamento.PENDENTE).get(0);
        RateioItem daSofia = rateioItemRepository.findByMoradorIdAndStatusPagamento(sofia.getId(), StatusPagamento.PENDENTE).get(0);

        mvc.perform(como(SOFIA, patch("/api/despesas/rateio/{id}/quitar", doLucas.getId()))).andExpect(status().isForbidden());
        mvc.perform(como(LUCAS, patch("/api/despesas/rateio/{id}/quitar", doLucas.getId()))).andExpect(status().isOk());
        mvc.perform(como(SINDICO, patch("/api/despesas/rateio/{id}/quitar", daSofia.getId()))).andExpect(status().isOk());
    }

    // ---------- Tarefas ----------

    @Test
    @DisplayName("Morador conclui só a própria tarefa; síndico conclui a de qualquer um")
    void concluirTarefa() throws Exception {
        TarefaDomestica doLucas = tarefaRepository.findByResponsavelId(lucas.getId()).stream()
                .filter(t -> t.getStatus() == StatusTarefa.PENDENTE).findFirst().orElseThrow();
        TarefaDomestica doMateus = tarefaRepository.findByStatus(StatusTarefa.PENDENTE).stream()
                .filter(t -> !t.getResponsavel().getId().equals(lucas.getId())).findFirst().orElseThrow();

        mvc.perform(como(SOFIA, patch("/api/tarefas/{id}/concluir", doLucas.getId()))).andExpect(status().isForbidden());
        mvc.perform(como(LUCAS, patch("/api/tarefas/{id}/concluir", doMateus.getId()))).andExpect(status().isForbidden());
        mvc.perform(como(LUCAS, patch("/api/tarefas/{id}/concluir", doLucas.getId()))).andExpect(status().isOk());
        mvc.perform(como(SINDICO, patch("/api/tarefas/{id}/concluir", doMateus.getId()))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tarefa atribuída é registrada em nome do síndico logado")
    void criarTarefaUsaSindicoLogado() throws Exception {
        String corpo = "{\"titulo\":\"Regar a horta\",\"dataPrazo\":\"2030-01-01\",\"responsavelId\":" + lucas.getId() + "}";

        mvc.perform(json(como(SINDICO, post("/api/tarefas")), corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atribuidaPorNome").value("Dra. Helena Vance"));
    }

    // ---------- Leituras de consumo ----------

    private String leitura(Long moradorId, String competencia, String kwh) {
        return "{" + (moradorId != null ? "\"moradorId\":" + moradorId + "," : "")
                + (competencia != null ? "\"competencia\":\"" + competencia + "\"," : "")
                + "\"kwh\":" + kwh + "}";
    }

    @Test
    @DisplayName("Morador registra a própria leitura, mas não a de outro; síndico registra a de qualquer um")
    void registrarLeitura() throws Exception {
        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, null, "150")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moradorNome").value("Lucas Silva"));

        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(sofia.getId(), null, "150")))
                .andExpect(status().isForbidden());

        mvc.perform(json(como(SINDICO, post("/api/leituras-consumo")), leitura(sofia.getId(), null, "150")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moradorNome").value("Sofia Chen"));

        mvc.perform(json(como(SINDICO, post("/api/leituras-consumo")), leitura(null, null, "150")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registrar de novo a leitura do mesmo mês corrige o valor, sem duplicar")
    void leituraDoMesmoMesCorrige() throws Exception {
        String competencia = java.time.YearMonth.now().minusMonths(2).toString();

        String primeira = mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, competencia, "100")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, competencia, "180.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kwh").value(180.5));

        mvc.perform(como(LUCAS, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leituras-consumo").param("competencia", competencia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        org.junit.jupiter.api.Assertions.assertTrue(primeira.contains("\"competencia\":\"" + competencia + "\""));
    }

    @Test
    @DisplayName("Leitura inválida é recusada: mês futuro, formato errado e consumo negativo")
    void leituraInvalida() throws Exception {
        String futuro = java.time.YearMonth.now().plusMonths(1).toString();

        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, futuro, "100")))
                .andExpect(status().isBadRequest());
        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, "2026/09", "100")))
                .andExpect(status().isBadRequest());
        mvc.perform(json(como(LUCAS, post("/api/leituras-consumo")), leitura(null, null, "-5")))
                .andExpect(status().isBadRequest());
        mvc.perform(como(LUCAS, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leituras-consumo").param("competencia", "lixo")))
                .andExpect(status().isBadRequest());
    }

    // ---------- Identidade: o corpo da requisição não decide quem você é ----------

    private static final String DESPESA = "{\"titulo\":\"Teste\",\"valorTotal\":50.00,\"dataVencimento\":\"2030-01-01\","
            + "\"categoria\":\"INTERNET_QUANTICA\",\"tipoDespesa\":\"FIXA\",\"tipoRateio\":\"IGUALITARIO\"%s}";

    @Test
    @DisplayName("Despesa lançada por morador sempre fica em nome dele, mesmo que o corpo tente forjar outro solicitante")
    void lancarDespesaNaoPermiteImpersonar() throws Exception {
        String forjado = String.format(DESPESA, ",\"solicitanteId\":" + sofia.getId());

        mvc.perform(json(como(LUCAS, post("/api/despesas")), forjado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitanteNome").value("Lucas Silva"));
    }

    @Test
    @DisplayName("Despesa lançada pelo síndico é administrativa")
    void lancarDespesaDoSindico() throws Exception {
        mvc.perform(json(como(SINDICO, post("/api/despesas")), String.format(DESPESA, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitanteNome").value("Lançamento Administrativo"));
    }

    @Test
    @DisplayName("Quem aprova é o síndico logado, mesmo que o corpo tente forjar outro aprovador")
    void avaliarDespesaUsaSindicoLogado() throws Exception {
        Despesa pendente = despesaRepository.findByStatus(StatusDespesa.PENDENTE_APROVACAO).get(0);
        String forjado = "{\"aprovado\":true,\"sindicoId\":" + lucas.getId() + "}";

        mvc.perform(json(como(SINDICO, post("/api/despesas/{id}/avaliar", pendente.getId())), forjado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovadorNome").value("Dra. Helena Vance"));
    }
}
