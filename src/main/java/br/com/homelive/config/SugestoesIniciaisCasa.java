package br.com.homelive.config;

import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.TarefaRecorrente;
import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoInfracao;
import br.com.homelive.model.enums.TipoRodizio;
import br.com.homelive.repository.EspacoComumRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * "Pacote inicial" de uma residência: espaços comuns, regras de penalidade e escalas de tarefas já prontos,
 * para o síndico não começar do zero. Serve ao cadastro (checkbox "Começar com sugestões prontas")
 * e aos dados de demonstração. O síndico edita tudo depois pelas telas do sistema.
 */
@Component
@RequiredArgsConstructor
public class SugestoesIniciaisCasa {

    private final EspacoComumRepository espacoComumRepository;
    private final RegraPenalidadeRepository regraPenalidadeRepository;
    private final TarefaRecorrenteRepository tarefaRecorrenteRepository;

    /** Cria os três grupos de sugestões. */
    public void aplicar() {
        criarEspacos();
        criarRegras();
        criarEscalas();
    }

    public List<EspacoComum> criarEspacos() {
        return espacoComumRepository.saveAll(List.of(
                new EspacoComum(
                        "Escritório Acústico VR",
                        "Cabine individual com cancelamento de ruído holográfico e conexão neural de alta fidelidade",
                        2,
                        "Máximo de 4 horas contínuas. Manter limpo após o uso."),
                new EspacoComum(
                        "Lavanderia Inteligente IoT",
                        "Lavadoras e secadoras ecológicas por ultra-som com esterilização UV automática",
                        4,
                        "Retirar roupas em até 10 minutos após o término do ciclo do robô."),
                new EspacoComum(
                        "Espaço Gourmet e Rooftop Hidropônico",
                        "Cozinha automatizada com fornos de convecção quântica e vista para a horta vertical",
                        12,
                        "Reserva antecipada necessária. Lixo orgânico direcionado ao biodigestor.")));
    }

    public void criarRegras() {
        regraPenalidadeRepository.saveAll(List.of(
                new RegraPenalidade("Tarefa atrasada (progressiva)", TipoInfracao.TAREFA_ATRASADA,
                        ModoCalculoPenalidade.PROGRESSIVA, 10, 5, 30, 30, null),
                new RegraPenalidade("Cancelamento em cima da hora", TipoInfracao.CANCELAMENTO_TARDIO,
                        ModoCalculoPenalidade.FIXA, 10, null, null, null, 2),
                new RegraPenalidade("Reserva não utilizada", TipoInfracao.NAO_COMPARECIMENTO,
                        ModoCalculoPenalidade.FIXA, 20, null, null, null, null)));
    }

    /** Escalas com rodízio. As duas primeiras ficam devidas hoje; a terceira, daqui a uma semana. */
    public void criarEscalas() {
        tarefaRecorrenteRepository.saveAll(List.of(
                new TarefaRecorrente("Retirada do lixo orgânico", "Levar ao biodigestor do térreo",
                        Periodicidade.SEMANAL, 2, 20, 10, TipoRodizio.SEQUENCIAL, LocalDate.now()),
                new TarefaRecorrente("Limpeza do Espaço Gourmet", "Higienizar bancadas e a cozinha automatizada",
                        Periodicidade.SEMANAL, 3, 30, 15, TipoRodizio.MENOR_CARGA, LocalDate.now()),
                new TarefaRecorrente("Troca dos filtros de água reciclada", "Substituir os cartuchos do sistema central",
                        Periodicidade.MENSAL, 5, 40, 20, TipoRodizio.MENOR_PONTUACAO, LocalDate.now().plusDays(7))));
    }
}
