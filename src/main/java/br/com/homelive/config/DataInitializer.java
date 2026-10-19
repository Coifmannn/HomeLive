package br.com.homelive.config;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.dto.DespesaDTO;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.LeituraConsumo;
import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.TarefaRecorrente;
import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoInfracao;
import br.com.homelive.model.enums.TipoRodizio;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import br.com.homelive.repository.EspacoComumRepository;
import br.com.homelive.repository.LeituraConsumoRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import br.com.homelive.service.AgendamentoService;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.DespesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MoradorRepository moradorRepository;
    private final SindicoRepository sindicoRepository;
    private final EspacoComumRepository espacoComumRepository;
    private final DespesaService despesaService;
    private final AgendamentoService agendamentoService;
    private final TarefaDomesticaRepository tarefaRepository;
    private final TarefaRecorrenteRepository tarefaRecorrenteRepository;
    private final LeituraConsumoRepository leituraConsumoRepository;
    private final RegraPenalidadeRepository regraPenalidadeRepository;
    private final PasswordEncoder passwordEncoder;

    /** Senha única das contas de demonstração. Só para desenvolvimento. */
    public static final String SENHA_DEMO = "homelive123";

    @Override
    public void run(String... args) throws Exception {
        if (moradorRepository.count() > 0) {
            return;
        }

        System.out.println(">>> [HomeLive 2050] Inicializando dados de demonstração POO...");

        String senhaDemo = passwordEncoder.encode(SENHA_DEMO);

        // 1. Cadastrar Síndico
        Sindico sindico = new Sindico("Dra. Helena Vance", "helena.vance@homelive2050.io", senhaDemo, "(11) 98765-4321", "Governança e IA");
        Sindico sindicoSalvo = sindicoRepository.save(sindico);

        // 2. Cadastrar Moradores
        Morador m1 = moradorRepository.save(new Morador("Lucas Silva", "lucas.silva@homelive2050.io", senhaDemo, "(11) 99111-0001", "Suíte 101-A", 30));
        Morador m2 = moradorRepository.save(new Morador("Sofia Chen", "sofia.chen@homelive2050.io", senhaDemo, "(11) 99111-0002", "Suíte 102-B", 28));
        Morador m3 = moradorRepository.save(new Morador("Mateus Oliveira", "mateus.oliveira@homelive2050.io", senhaDemo, "(11) 99111-0003", "Suíte 201-A", 15));
        Morador m4 = moradorRepository.save(new Morador("Beatriz Santos", "beatriz.santos@homelive2050.io", senhaDemo, "(11) 99111-0004", "Suíte 202-B", 30));

        // Ajusta pontuações iniciais para gamificação
        m2.adicionarPontos(30); // 130 pts
        m1.adicionarPontos(15); // 115 pts
        m3.deduzirPontos(10);   // 90 pts
        moradorRepository.saveAll(java.util.List.of(m1, m2, m3, m4));

        // Leituras mensais de consumo: alimentam o rateio por consumo, sempre do mês da conta.
        YearMonth mesAtual = YearMonth.now();
        YearMonth mesAnterior = mesAtual.minusMonths(1);
        leituraConsumoRepository.saveAll(java.util.List.of(
                new LeituraConsumo(m1, mesAnterior, new BigDecimal("120.00")),
                new LeituraConsumo(m2, mesAnterior, new BigDecimal("100.00")),
                new LeituraConsumo(m3, mesAnterior, new BigDecimal("70.00")),
                new LeituraConsumo(m4, mesAnterior, new BigDecimal("150.00")),
                new LeituraConsumo(m1, mesAtual, new BigDecimal("140.00")),
                new LeituraConsumo(m2, mesAtual, new BigDecimal("95.00")),
                new LeituraConsumo(m3, mesAtual, new BigDecimal("60.00")),
                new LeituraConsumo(m4, mesAtual, new BigDecimal("165.00"))
        ));

        // 3. Cadastrar Espaços Comuns
        EspacoComum espaco1 = espacoComumRepository.save(new EspacoComum(
                "Escritório Acústico VR",
                "Cabine individual com cancelamento de ruído holográfico e conexão neural de alta fidelidade",
                2,
                "Máximo de 4 horas contínuas. Manter limpo após o uso."
        ));

        EspacoComum espaco2 = espacoComumRepository.save(new EspacoComum(
                "Lavanderia Inteligente IoT",
                "Lavadoras e secadoras ecológicas por ultra-som com esterilização UV automática",
                4,
                "Retirar roupas em até 10 minutos após o término do ciclo do robô."
        ));

        EspacoComum espaco3 = espacoComumRepository.save(new EspacoComum(
                "Espaço Gourmet e Rooftop Hidropônico",
                "Cozinha automatizada com fornos de convecção quântica e vista para a horta vertical",
                12,
                "Reserva antecipada necessária. Lixo orgânico direcionado ao biodigestor."
        ));

        // 4. Lançar e Processar Despesas com Diferentes Strategies
        // Despesa A: Internet Quântica (Rateio Igualitário)
        DespesaDTO.Request desp1 = new DespesaDTO.Request(
                "Conexão Quântica 100 Gbps",
                "Link simétrico com proteção criptográfica de rede neural",
                new BigDecimal("400.00"),
                LocalDate.now().plusDays(10),
                CategoriaDespesa.INTERNET_QUANTICA,
                TipoDespesa.FIXA,
                TipoRateio.IGUALITARIO,
                null
        );
        DespesaDTO.Response r1 = despesaService.lancarDespesa(desp1, new UsuarioAutenticado(m1));
        despesaService.avaliarDespesa(r1.getId(), new DespesaDTO.AprovacaoRequest(true, null), sindicoSalvo.getId());

        // Despesa B: Matriz Solar e Células de Hidrogênio (Rateio por Consumo, conta do mês anterior)
        DespesaDTO.Request desp2 = new DespesaDTO.Request(
                "Consumo de Energia e Baterias de Fusão",
                "Fatura referente à medição inteligente de kWh consumidos",
                new BigDecimal("920.00"),
                LocalDate.now().plusDays(15),
                CategoriaDespesa.ENERGIA_SOLAR,
                TipoDespesa.VARIAVEL,
                TipoRateio.POR_CONSUMO,
                mesAnterior.toString()
        );
        DespesaDTO.Response r2 = despesaService.lancarDespesa(desp2, new UsuarioAutenticado(m2));
        despesaService.avaliarDespesa(r2.getId(), new DespesaDTO.AprovacaoRequest(true, null), sindicoSalvo.getId());

        // Despesa C: Manutenção Preventiva dos Drones de Limpeza (Pendente para o Síndico aprovar)
        DespesaDTO.Request desp3 = new DespesaDTO.Request(
                "Revisão Técnica dos Robôs Aspiradores",
                "Substituição de escovas ultrassônicas e calibração de sensores LiDAR",
                new BigDecimal("350.00"),
                LocalDate.now().plusDays(20),
                CategoriaDespesa.MANUTENCAO_ROBOS,
                TipoDespesa.VARIAVEL,
                TipoRateio.POR_ESTADIA,
                null
        );
        despesaService.lancarDespesa(desp3, new UsuarioAutenticado(m3));

        // 5. Cadastrar Agendamentos de Espaços
        LocalDateTime hojeTardeInicio = LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0);
        LocalDateTime hojeTardeFim = hojeTardeInicio.plusHours(2);
        agendamentoService.criarAgendamento(new AgendamentoDTO.Request(
                espaco1.getId(),
                m1.getId(),
                hojeTardeInicio,
                hojeTardeFim,
                "Apresentação de projeto no metaverso corporativo"
        ), new UsuarioAutenticado(m1));

        LocalDateTime amanhaInicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0);
        LocalDateTime amanhaFim = amanhaInicio.plusHours(1).plusMinutes(30);
        agendamentoService.criarAgendamento(new AgendamentoDTO.Request(
                espaco2.getId(),
                m2.getId(),
                amanhaInicio,
                amanhaFim,
                "Higienização de roupas de algodão bio-sintético"
        ), new UsuarioAutenticado(m2));

        // 6. Cadastrar Tarefas Domésticas com Gamificação
        TarefaDomestica t1 = new TarefaDomestica(
                "Calibração dos Sensores dos Robôs Aspiradores",
                "Garantir alinhamento dos emissores laser da base do corredor principal",
                LocalDate.now().plusDays(2),
                25,
                15,
                m2,
                sindicoSalvo
        );
        t1.concluir(); // Sofia concluiu e pontuou
        tarefaRepository.save(t1);

        TarefaDomestica t2 = new TarefaDomestica(
                "Higienização dos Filtros Hidropônicos do Rooftop",
                "Lavagem preventiva dos reservatórios de nutrientes da horta",
                LocalDate.now().plusDays(4),
                30,
                20,
                m1,
                sindicoSalvo
        );
        tarefaRepository.save(t2);

        TarefaDomestica t3 = new TarefaDomestica(
                "Triagem e Descarte de Metais Raros",
                "Organização das gavetas de lixo eletrônico na garagem",
                LocalDate.now().minusDays(1), // Vencida para testar detecção de atraso
                20,
                15,
                m3,
                sindicoSalvo
        );
        tarefaRepository.save(t3);

        // 6.1 Regras de penalidade configuradas pelo síndico
        regraPenalidadeRepository.saveAll(java.util.List.of(
                new RegraPenalidade("Tarefa atrasada (progressiva)", TipoInfracao.TAREFA_ATRASADA,
                        ModoCalculoPenalidade.PROGRESSIVA, 10, 5, 30, 30, null),
                new RegraPenalidade("Cancelamento em cima da hora", TipoInfracao.CANCELAMENTO_TARDIO,
                        ModoCalculoPenalidade.FIXA, 10, null, null, null, 2),
                new RegraPenalidade("Reserva não utilizada", TipoInfracao.NAO_COMPARECIMENTO,
                        ModoCalculoPenalidade.FIXA, 20, null, null, null, null)
        ));

        // 7. Tarefas recorrentes (escalas com rodízio). Devidas hoje: POST /api/tarefas-recorrentes/gerar as distribui.
        tarefaRecorrenteRepository.saveAll(java.util.List.of(
                new TarefaRecorrente("Retirada do lixo orgânico", "Levar ao biodigestor do térreo",
                        Periodicidade.SEMANAL, 2, 20, 10, TipoRodizio.SEQUENCIAL, LocalDate.now()),
                new TarefaRecorrente("Limpeza do Espaço Gourmet", "Higienizar bancadas e a cozinha automatizada",
                        Periodicidade.SEMANAL, 3, 30, 15, TipoRodizio.MENOR_CARGA, LocalDate.now()),
                new TarefaRecorrente("Troca dos filtros de água reciclada", "Substituir os cartuchos do sistema central",
                        Periodicidade.MENSAL, 5, 40, 20, TipoRodizio.MENOR_PONTUACAO, LocalDate.now().plusDays(7))
        ));

        System.out.println(">>> [HomeLive 2050] Dados de teste carregados com sucesso!");
    }
}
