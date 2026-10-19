package br.com.homelive.service;

import br.com.homelive.dto.PenalidadeDTO;
import br.com.homelive.dto.RegraPenalidadeDTO;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Penalidade;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.StatusAgendamento;
import br.com.homelive.model.enums.TipoInfracao;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.PenalidadeRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import br.com.homelive.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Penalidades configuráveis (fluxo completo com banco)")
class PenalidadesIntegracaoTest {

    @Autowired TarefaService tarefaService;
    @Autowired AgendamentoService agendamentoService;
    @Autowired PenalidadeService penalidadeService;
    @Autowired RegraPenalidadeService regraService;
    @Autowired TarefaDomesticaRepository tarefaRepository;
    @Autowired AgendamentoEspacoRepository agendamentoRepository;
    @Autowired PenalidadeRepository penalidadeRepository;
    @Autowired RegraPenalidadeRepository regraRepository;
    @Autowired MoradorRepository moradorRepository;
    @Autowired SindicoRepository sindicoRepository;

    private Morador lucas;
    private Morador sofia;
    private Sindico helena;

    @BeforeEach
    void setUp() {
        lucas = moradorRepository.findAll().stream().filter(m -> m.getNome().equals("Lucas Silva")).findFirst().orElseThrow();
        sofia = moradorRepository.findAll().stream().filter(m -> m.getNome().equals("Sofia Chen")).findFirst().orElseThrow();
        helena = sindicoRepository.findAll().get(0);
    }

    private void criarTarefasVencidasDoLucas(int quantidade) {
        for (int i = 1; i <= quantidade; i++) {
            tarefaRepository.save(new TarefaDomestica("Vencida " + i, "x", LocalDate.now().minusDays(1), 25, 15, lucas, null));
        }
        tarefaRepository.flush();
    }

    private List<Penalidade> penalidadesDe(Morador m, TipoInfracao tipo) {
        return penalidadeRepository.findByMoradorIdOrderByDataAplicacaoDescIdDesc(m.getId()).stream()
                .filter(p -> p.getTipoInfracao() == tipo).toList();
    }

    // ---------- Tarefa atrasada ----------

    @Test
    @DisplayName("Tarefa atrasada: a regra progressiva cobra mais a cada reincidência")
    void tarefaAtrasadaProgressiva() {
        int antes = lucas.getPontosConvivencia();
        criarTarefasVencidasDoLucas(4);

        tarefaService.verificarEProcessarAtrasos();

        // Regra de demonstração: base 10, +5 por reincidência, teto 30.
        List<Integer> cobrados = penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).stream()
                .map(Penalidade::getPontos).sorted().toList();
        assertEquals(List.of(10, 15, 20, 25), cobrados);
        assertEquals(antes - 70, lucas.getPontosConvivencia());
        assertTrue(penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).stream().allMatch(p -> p.getRegra() != null));
    }

    @Test
    @DisplayName("Tarefa atrasada: o teto limita a cobrança")
    void tarefaAtrasadaRespeitaTeto() {
        criarTarefasVencidasDoLucas(7);

        tarefaService.verificarEProcessarAtrasos();

        List<Integer> cobrados = penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).stream()
                .map(Penalidade::getPontos).sorted().toList();
        assertEquals(List.of(10, 15, 20, 25, 30, 30, 30), cobrados);
    }

    @Test
    @DisplayName("Penalidade antiga, fora da janela de reincidência, não conta como reincidência")
    void janelaDeReincidencia() {
        criarTarefasVencidasDoLucas(1);
        tarefaService.verificarEProcessarAtrasos();
        Penalidade antiga = penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).get(0);
        ReflectionTestUtils.setField(antiga, "dataAplicacao", LocalDateTime.now().minusDays(40));
        penalidadeRepository.saveAndFlush(antiga);

        criarTarefasVencidasDoLucas(1);
        tarefaService.verificarEProcessarAtrasos();

        List<Integer> cobrados = penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).stream().map(Penalidade::getPontos).toList();
        assertEquals(List.of(10, 10), cobrados, "a segunda recomeça do valor base");
    }

    @Test
    @DisplayName("Sem regra ativa, vale o valor de penalidade da própria tarefa")
    void semRegraUsaValorDaTarefa() {
        regraRepository.findAll().forEach(r -> r.desativar());
        regraRepository.flush();
        int antes = lucas.getPontosConvivencia();
        criarTarefasVencidasDoLucas(1);

        tarefaService.verificarEProcessarAtrasos();

        List<Penalidade> aplicadas = penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA);
        assertEquals(1, aplicadas.size());
        assertEquals(15, aplicadas.get(0).getPontos());
        assertNull(aplicadas.get(0).getRegra());
        assertEquals(antes - 15, lucas.getPontosConvivencia());
    }

    @Test
    @DisplayName("Processar duas vezes não pune duas vezes a mesma tarefa")
    void naoPuneDuasVezes() {
        criarTarefasVencidasDoLucas(1);

        tarefaService.verificarEProcessarAtrasos();
        tarefaService.verificarEProcessarAtrasos();

        assertEquals(1, penalidadesDe(lucas, TipoInfracao.TAREFA_ATRASADA).size());
    }

    // ---------- Reservas ----------

    private AgendamentoEspaco reservaDe(Morador m) {
        return agendamentoRepository.findByMoradorId(m.getId()).get(0);
    }

    @Test
    @DisplayName("Cancelar em cima da hora (dentro do limite de 2h) gera penalidade; o síndico cancelando não")
    void cancelamentoTardio() {
        // Reserva de demonstração do Lucas começa em menos de 2h.
        AgendamentoEspaco reserva = reservaDe(lucas);
        int antes = lucas.getPontosConvivencia();

        agendamentoService.cancelarAgendamento(reserva.getId(), new UsuarioAutenticado(lucas));

        List<Penalidade> aplicadas = penalidadesDe(lucas, TipoInfracao.CANCELAMENTO_TARDIO);
        assertEquals(1, aplicadas.size());
        assertEquals(10, aplicadas.get(0).getPontos());
        assertEquals(antes - 10, lucas.getPontosConvivencia());
    }

    @Test
    @DisplayName("Cancelar com antecedência suficiente não gera penalidade")
    void cancelamentoComAntecedencia() {
        AgendamentoEspaco reserva = reservaDe(sofia); // amanhã de manhã

        agendamentoService.cancelarAgendamento(reserva.getId(), new UsuarioAutenticado(sofia));

        assertTrue(penalidadesDe(sofia, TipoInfracao.CANCELAMENTO_TARDIO).isEmpty());
    }

    @Test
    @DisplayName("Cancelamento feito pelo síndico, mesmo em cima da hora, não penaliza o morador")
    void cancelamentoDoSindico() {
        AgendamentoEspaco reserva = reservaDe(lucas);

        agendamentoService.cancelarAgendamento(reserva.getId(), new UsuarioAutenticado(helena));

        assertTrue(penalidadesDe(lucas, TipoInfracao.CANCELAMENTO_TARDIO).isEmpty());
    }

    @Test
    @DisplayName("Sem regra ativa de cancelamento tardio, ninguém é penalizado")
    void cancelamentoSemRegra() {
        regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.CANCELAMENTO_TARDIO).orElseThrow().desativar();
        regraRepository.flush();

        agendamentoService.cancelarAgendamento(reservaDe(lucas).getId(), new UsuarioAutenticado(lucas));

        assertTrue(penalidadesDe(lucas, TipoInfracao.CANCELAMENTO_TARDIO).isEmpty());
    }

    @Test
    @DisplayName("Não comparecimento: o síndico registra depois do início e o morador é penalizado")
    void naoComparecimento() {
        AgendamentoEspaco reserva = reservaDe(lucas);
        int antes = lucas.getPontosConvivencia();

        // Antes de a reserva começar, não dá para registrar.
        assertThrows(RegraNegocioException.class, () -> agendamentoService.registrarNaoComparecimento(reserva.getId()));
        assertTrue(penalidadesDe(lucas, TipoInfracao.NAO_COMPARECIMENTO).isEmpty());

        // Passa o tempo: a reserva já começou.
        ReflectionTestUtils.setField(reserva, "dataHoraInicio", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(reserva, "dataHoraFim", LocalDateTime.now().plusHours(1));
        agendamentoRepository.saveAndFlush(reserva);

        assertEquals(StatusAgendamento.NAO_COMPARECEU, agendamentoService.registrarNaoComparecimento(reserva.getId()).getStatus());

        List<Penalidade> aplicadas = penalidadesDe(lucas, TipoInfracao.NAO_COMPARECIMENTO);
        assertEquals(1, aplicadas.size());
        assertEquals(20, aplicadas.get(0).getPontos());
        assertEquals(antes - 20, lucas.getPontosConvivencia());
    }

    // ---------- Manual e consultas ----------

    @Test
    @DisplayName("Penalidade manual: registra síndico, motivo e desconta os pontos")
    void penalidadeManual() {
        int antes = lucas.getPontosConvivencia();
        PenalidadeDTO.ManualRequest req = new PenalidadeDTO.ManualRequest(lucas.getId(), 12, "Barulho após as 22h");

        PenalidadeDTO.Response resposta = penalidadeService.aplicarManual(req, helena.getId());

        assertEquals(TipoInfracao.MANUAL, resposta.getTipoInfracao());
        assertEquals(12, resposta.getPontos());
        assertEquals("Dra. Helena Vance", resposta.getAplicadaPorNome());
        assertNull(resposta.getRegraId());
        assertEquals(antes - 12, lucas.getPontosConvivencia());
    }

    @Test
    @DisplayName("Penalidade automática aparece como aplicada pelo sistema")
    void penalidadeAutomaticaSemSindico() {
        criarTarefasVencidasDoLucas(1);
        tarefaService.verificarEProcessarAtrasos();

        PenalidadeDTO.Response resposta = penalidadeService.listarPorMorador(lucas.getId(), new UsuarioAutenticado(lucas)).get(0);

        assertEquals("Sistema Automático", resposta.getAplicadaPorNome());
        assertNotNull(resposta.getRegraNome());
    }

    @Test
    @DisplayName("Morador consulta só o próprio histórico; o síndico consulta o de qualquer um")
    void historicoSoDoDono() {
        penalidadeService.aplicarManual(new PenalidadeDTO.ManualRequest(lucas.getId(), 5, "x"), helena.getId());

        assertEquals(1, penalidadeService.listarPorMorador(lucas.getId(), new UsuarioAutenticado(lucas)).size());
        assertEquals(1, penalidadeService.listarPorMorador(lucas.getId(), new UsuarioAutenticado(helena)).size());
        assertThrows(AccessDeniedException.class,
                () -> penalidadeService.listarPorMorador(lucas.getId(), new UsuarioAutenticado(sofia)));
    }

    // ---------- Configuração das regras ----------

    private RegraPenalidadeDTO.Request pedido(TipoInfracao tipo, ModoCalculoPenalidade modo, int base) {
        RegraPenalidadeDTO.Request r = new RegraPenalidadeDTO.Request();
        r.setNome("Nova regra");
        r.setTipoInfracao(tipo);
        r.setModoCalculo(modo);
        r.setPontosBase(base);
        return r;
    }

    @Test
    @DisplayName("Só uma regra ativa por tipo de infração")
    void umaRegraAtivaPorTipo() {
        assertThrows(RegraNegocioException.class,
                () -> regraService.criar(pedido(TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, 50)));
    }

    @Test
    @DisplayName("Trocar a regra: desativa a atual, cria a nova, e ela passa a valer")
    void trocarDeRegra() {
        Long atual = regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.NAO_COMPARECIMENTO).orElseThrow().getId();
        regraService.desativar(atual);
        regraService.criar(pedido(TipoInfracao.NAO_COMPARECIMENTO, ModoCalculoPenalidade.FIXA, 50));

        AgendamentoEspaco reserva = reservaDe(lucas);
        ReflectionTestUtils.setField(reserva, "dataHoraInicio", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(reserva, "dataHoraFim", LocalDateTime.now().plusHours(1));
        agendamentoRepository.saveAndFlush(reserva);
        agendamentoService.registrarNaoComparecimento(reserva.getId());

        assertEquals(50, penalidadesDe(lucas, TipoInfracao.NAO_COMPARECIMENTO).get(0).getPontos());
        assertThrows(RegraNegocioException.class, () -> regraService.ativar(atual),
                "reativar a antiga conflita com a nova que já está ativa");
    }

    @Test
    @DisplayName("Regra inválida é recusada na criação")
    void regraInvalida() {
        regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.TAREFA_ATRASADA).orElseThrow().desativar();
        regraRepository.flush();

        assertThrows(RegraNegocioException.class,
                () -> regraService.criar(pedido(TipoInfracao.TAREFA_ATRASADA, ModoCalculoPenalidade.PROGRESSIVA, 10)),
                "progressiva sem incremento");
        assertThrows(RegraNegocioException.class,
                () -> regraService.criar(pedido(TipoInfracao.MANUAL, ModoCalculoPenalidade.FIXA, 10)),
                "manual não tem regra");
    }
}
