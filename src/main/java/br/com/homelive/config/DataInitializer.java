package br.com.homelive.config;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.dto.DespesaDTO;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.LeituraConsumo;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.TarefaRecorrente;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoRodizio;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import br.com.homelive.repository.LeituraConsumoRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import br.com.homelive.service.AgendamentoService;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.CasaService;
import br.com.homelive.service.DespesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MoradorRepository moradorRepository;
    private final SindicoRepository sindicoRepository;
    private final DespesaService despesaService;
    private final AgendamentoService agendamentoService;
    private final TarefaDomesticaRepository tarefaRepository;
    private final LeituraConsumoRepository leituraConsumoRepository;
    private final TarefaRecorrenteRepository tarefaRecorrenteRepository;
    private final SugestoesIniciaisCasa sugestoes;
    private final CasaService casaService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Liga/desliga os dados de demonstração (variável de ambiente {@code DEMO_DATA}). Desligado, o sistema abre
     * vazio e o primeiro cadastro cria a residência.
     */
    @Value("${homelive.demo-data:true}")
    private boolean demoData;

    /** Senha única das contas de demonstração. Só para desenvolvimento. */
    public static final String SENHA_DEMO = "homelive123";

    @Override
    public void run(String... args) throws Exception {
        if (!demoData || moradorRepository.count() > 0) {
            // Sem semente: só garante que um banco antigo (com usuários e sem casa) não pareça "vazio".
            casaService.garantirCasaSeHouverUsuarios();
            return;
        }

        System.out.println(">>> [HomeLive 2050] Inicializando dados de demonstração POO...");

        casaService.criar("Residência HomeLive 2050");

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

        // 3. Espaços Comuns (as mesmas sugestões que o cadastro oferece ao criar uma residência)
        List<EspacoComum> espacos = sugestoes.criarEspacos();
        EspacoComum espaco1 = espacos.get(0);
        EspacoComum espaco2 = espacos.get(1);

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
        sugestoes.criarRegras();

        // 7. Tarefas recorrentes (escalas com rodízio). Devidas hoje: POST /api/tarefas-recorrentes/gerar as distribui.
        sugestoes.criarEscalas();

        // 7.1 Escalas extras de exemplo (só na demonstração, não entram no pacote inicial do cadastro).
        tarefaRecorrenteRepository.saveAll(List.of(
                new TarefaRecorrente("Limpeza dos banheiros comuns", "Higienizar pias, vasos e box com os robôs de apoio",
                        Periodicidade.SEMANAL, 2, 25, 15, TipoRodizio.SEQUENCIAL, LocalDate.now()),
                new TarefaRecorrente("Regar a horta vertical", "Conferir o nível dos reservatórios e regar o rooftop",
                        Periodicidade.DIARIA, 1, 10, 5, TipoRodizio.SEQUENCIAL, LocalDate.now().plusDays(1)),
                new TarefaRecorrente("Reposição de suprimentos de limpeza", "Conferir estoque de detergente, papel e sacos de lixo",
                        Periodicidade.QUINZENAL, 4, 20, 10, TipoRodizio.MENOR_CARGA, LocalDate.now().plusDays(3))));

        // 8. Gastos extras de exemplo, em estados diferentes do ciclo de vida.
        // Despesa D: Água (Rateio Igualitário, já aprovada e rateada)
        DespesaDTO.Response r4 = despesaService.lancarDespesa(new DespesaDTO.Request(
                "Conta de Água Reutilizável",
                "Consumo do ciclo de captação e tratamento de água de chuva",
                new BigDecimal("180.00"),
                LocalDate.now().plusDays(8),
                CategoriaDespesa.AGUA_REUTILIZAVEL,
                TipoDespesa.FIXA,
                TipoRateio.IGUALITARIO,
                null
        ), new UsuarioAutenticado(m4));
        despesaService.avaliarDespesa(r4.getId(), new DespesaDTO.AprovacaoRequest(true, null), sindicoSalvo.getId());

        // Despesa E: Compras coletivas (Rateio por Estadia, já aprovada e rateada)
        DespesaDTO.Response r5 = despesaService.lancarDespesa(new DespesaDTO.Request(
                "Compras Coletivas de Alimentos",
                "Cesta mensal de hortifrúti e itens básicos da despensa inteligente",
                new BigDecimal("560.00"),
                LocalDate.now().plusDays(5),
                CategoriaDespesa.ALIMENTACAO_INTELIGENTE,
                TipoDespesa.VARIAVEL,
                TipoRateio.POR_ESTADIA,
                null
        ), new UsuarioAutenticado(m1));
        despesaService.avaliarDespesa(r5.getId(), new DespesaDTO.AprovacaoRequest(true, null), sindicoSalvo.getId());

        // Despesa F: Taxa geral (pendente, para o síndico avaliar)
        despesaService.lancarDespesa(new DespesaDTO.Request(
                "Taxa Geral do Condomínio",
                "Limpeza das áreas comuns, segurança e jardinagem",
                new BigDecimal("640.00"),
                LocalDate.now().plusDays(12),
                CategoriaDespesa.CONDOMINIO_GERAL,
                TipoDespesa.FIXA,
                TipoRateio.IGUALITARIO,
                null
        ), new UsuarioAutenticado(m2));

        // Despesa G: Reparo urgente rejeitado pelo síndico (exemplo do fluxo de rejeição)
        DespesaDTO.Response r7 = despesaService.lancarDespesa(new DespesaDTO.Request(
                "Troca de Painel Solar Decorativo",
                "Substituição estética sem ganho de geração",
                new BigDecimal("1200.00"),
                LocalDate.now().plusDays(30),
                CategoriaDespesa.ENERGIA_SOLAR,
                TipoDespesa.VARIAVEL,
                TipoRateio.IGUALITARIO,
                null
        ), new UsuarioAutenticado(m3));
        despesaService.avaliarDespesa(r7.getId(),
                new DespesaDTO.AprovacaoRequest(false, "Gasto não essencial; reapresentar com orçamento comparativo."),
                sindicoSalvo.getId());

        System.out.println(">>> [HomeLive 2050] Dados de teste carregados com sucesso!");
    }
}
