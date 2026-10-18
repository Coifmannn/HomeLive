package br.com.coliving.service;

import br.com.coliving.dto.TarefaDTO;
import br.com.coliving.exception.RecursoNaoEncontradoException;
import br.com.coliving.exception.RegraNegocioException;
import br.com.coliving.model.Morador;
import br.com.coliving.model.Sindico;
import br.com.coliving.model.TarefaDomestica;
import br.com.coliving.model.enums.StatusTarefa;
import br.com.coliving.repository.MoradorRepository;
import br.com.coliving.repository.TarefaDomesticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaDomesticaRepository tarefaRepository;
    private final MoradorService moradorService;
    private final SindicoService sindicoService;
    private final MoradorRepository moradorRepository;

    @Transactional
    public TarefaDTO.Response criarTarefa(TarefaDTO.Request request) {
        Morador responsavel = null;
        if (request.getResponsavelId() != null) {
            responsavel = moradorService.buscarEntidade(request.getResponsavelId());
        }

        Sindico atribuidaPor = null;
        if (request.getAtribuidaPorId() != null) {
            atribuidaPor = sindicoService.buscarEntidade(request.getAtribuidaPorId());
        }

        TarefaDomestica tarefa = new TarefaDomestica(
                request.getTitulo(),
                request.getDescricao(),
                request.getDataPrazo(),
                request.getPontosRecompensa(),
                request.getPontosPenalidade(),
                responsavel,
                atribuidaPor
        );

        TarefaDomestica salva = tarefaRepository.save(tarefa);
        return toResponse(salva);
    }

    @Transactional
    public TarefaDTO.Response concluirTarefa(Long tarefaId) {
        TarefaDomestica tarefa = buscarEntidade(tarefaId);

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new RegraNegocioException("Esta tarefa já foi concluída anteriormente.");
        }

        tarefa.concluir();
        if (tarefa.getResponsavel() != null) {
            moradorRepository.save(tarefa.getResponsavel());
        }

        TarefaDomestica atualizada = tarefaRepository.save(tarefa);
        return toResponse(atualizada);
    }

    @Transactional
    public int verificarEProcessarAtrasos() {
        List<TarefaDomestica> atrasadas = tarefaRepository.findByStatusAndDataPrazoBefore(
                StatusTarefa.PENDENTE,
                LocalDate.now()
        );

        for (TarefaDomestica tarefa : atrasadas) {
            tarefa.penalizarAtraso();
            if (tarefa.getResponsavel() != null) {
                moradorRepository.save(tarefa.getResponsavel());
            }
            tarefaRepository.save(tarefa);
        }

        return atrasadas.size();
    }

    public List<TarefaDTO.Response> listarTodas() {
        return tarefaRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<TarefaDTO.Response> listarPorMorador(Long moradorId) {
        return tarefaRepository.findByResponsavelId(moradorId).stream().map(this::toResponse).toList();
    }

    public List<TarefaDTO.Response> listarPorStatus(StatusTarefa status) {
        return tarefaRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public TarefaDomestica buscarEntidade(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada com o ID: " + id));
    }

    public TarefaDTO.Response toResponse(TarefaDomestica t) {
        return new TarefaDTO.Response(
                t.getId(),
                t.getTitulo(),
                t.getDescricao(),
                t.getDataPrazo(),
                t.getPontosRecompensa(),
                t.getPontosPenalidade(),
                t.getStatus(),
                t.getResponsavel() != null ? t.getResponsavel().getId() : null,
                t.getResponsavel() != null ? t.getResponsavel().getNome() : "Não atribuída",
                t.getAtribuidaPor() != null ? t.getAtribuidaPor().getNome() : "Sistema Automático",
                t.getDataConclusao()
        );
    }
}
