package br.com.homelive.service;

import br.com.homelive.dto.TarefaDTO;
import br.com.homelive.dto.TarefaRecorrenteDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.Morador;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.TarefaRecorrente;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import br.com.homelive.strategy.ContextoRodizio;
import br.com.homelive.strategy.RodizioStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TarefaRecorrenteService {

    private final TarefaRecorrenteRepository recorrenteRepository;
    private final TarefaDomesticaRepository tarefaRepository;
    private final MoradorRepository moradorRepository;
    private final RodizioStrategyFactory rodizioStrategyFactory;
    private final TarefaService tarefaService;

    @Transactional
    public TarefaRecorrenteDTO.Response criar(TarefaRecorrenteDTO.Request request) {
        TarefaRecorrente recorrente = new TarefaRecorrente(
                request.getTitulo(),
                request.getDescricao(),
                request.getPeriodicidade(),
                request.getPrazoEmDias(),
                request.getPontosRecompensa(),
                request.getPontosPenalidade(),
                request.getTipoRodizio(),
                request.getPrimeiraGeracao()
        );
        return toResponse(recorrenteRepository.save(recorrente));
    }

    public List<TarefaRecorrenteDTO.Response> listarTodas() {
        return recorrenteRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TarefaRecorrenteDTO.Response obterPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public TarefaRecorrenteDTO.Response ativar(Long id) {
        TarefaRecorrente recorrente = buscarEntidade(id);
        recorrente.ativar();
        return toResponse(recorrente);
    }

    @Transactional
    public TarefaRecorrenteDTO.Response desativar(Long id) {
        TarefaRecorrente recorrente = buscarEntidade(id);
        recorrente.desativar();
        return toResponse(recorrente);
    }

    /**
     * Gera as tarefas de todos os modelos devidos hoje, escolhendo o responsável pela estratégia de
     * rodízio de cada um. É idempotente: depois de gerar, o modelo avança a data e não dispara de novo.
     */
    @Transactional
    public List<TarefaDTO.Response> gerarTarefasDevidas() {
        LocalDate hoje = LocalDate.now();

        List<TarefaRecorrente> devidas = recorrenteRepository.findByAtivaTrueOrderByIdAsc().stream()
                .filter(r -> r.devidaEm(hoje))
                .toList();
        if (devidas.isEmpty()) {
            return List.of();
        }

        List<Morador> candidatos = moradorRepository.findByAtivoTrue().stream()
                .sorted(Comparator.comparing(Morador::getId))
                .toList();
        if (candidatos.isEmpty()) {
            // Nada a fazer hoje: os modelos continuam devidos e a rotina tenta de novo amanhã.
            return List.of();
        }
        Map<Long, Long> cargaAberta = carregarTarefasAbertas();

        List<TarefaDTO.Response> geradas = new ArrayList<>();
        for (TarefaRecorrente recorrente : devidas) {
            ContextoRodizio contexto = new ContextoRodizio(
                    recorrente.getUltimoResponsavel() != null ? recorrente.getUltimoResponsavel().getId() : null,
                    cargaAberta);

            Morador escolhido = rodizioStrategyFactory.obterStrategy(recorrente.getTipoRodizio())
                    .escolher(candidatos, contexto);

            TarefaDomestica tarefa = tarefaRepository.save(recorrente.gerarTarefa(escolhido, hoje));
            // A tarefa recém-criada já pesa na carga das próximas escolhas desta mesma rodada.
            cargaAberta.merge(escolhido.getId(), 1L, Long::sum);
            geradas.add(tarefaService.toResponse(tarefa));
        }
        return geradas;
    }

    /** Tarefas ainda não cumpridas (pendentes ou atrasadas), contadas por responsável. Mapa mutável. */
    private Map<Long, Long> carregarTarefasAbertas() {
        return Stream.concat(
                        tarefaRepository.findByStatus(StatusTarefa.PENDENTE).stream(),
                        tarefaRepository.findByStatus(StatusTarefa.ATRASADA).stream())
                .filter(t -> t.getResponsavel() != null)
                .collect(Collectors.groupingBy(t -> t.getResponsavel().getId(), HashMap::new, Collectors.counting()));
    }

    public TarefaRecorrente buscarEntidade(Long id) {
        return recorrenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa recorrente não encontrada com o ID: " + id));
    }

    public TarefaRecorrenteDTO.Response toResponse(TarefaRecorrente r) {
        return new TarefaRecorrenteDTO.Response(
                r.getId(),
                r.getTitulo(),
                r.getDescricao(),
                r.getPeriodicidade(),
                r.getPrazoEmDias(),
                r.getPontosRecompensa(),
                r.getPontosPenalidade(),
                r.getTipoRodizio(),
                r.getProximaGeracao(),
                r.isAtiva(),
                r.getUltimoResponsavel() != null ? r.getUltimoResponsavel().getNome() : null
        );
    }
}
