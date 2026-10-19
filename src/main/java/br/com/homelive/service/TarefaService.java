package br.com.homelive.service;

import br.com.homelive.dto.TarefaDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.event.TarefaMarcadaComoAtrasada;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TarefaDTO.Response criarTarefa(TarefaDTO.Request request, Long sindicoId) {
        Morador responsavel = null;
        if (request.getResponsavelId() != null) {
            responsavel = moradorService.buscarEntidade(request.getResponsavelId());
        }

        Sindico atribuidaPor = sindicoService.buscarEntidade(sindicoId);

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
    public TarefaDTO.Response concluirTarefa(Long tarefaId, UsuarioAutenticado ator) {
        TarefaDomestica tarefa = buscarEntidade(tarefaId);

        // Tarefa sem responsável não tem dono: só o síndico conclui.
        ator.exigirSindicoOuDono(tarefa.getResponsavel() != null ? tarefa.getResponsavel().getId() : null);

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
            if (tarefa.marcarComoAtrasada()) {
                tarefaRepository.save(tarefa);
                // Quem quiser reagir (as regras de penalidade, por exemplo) escuta o evento.
                eventPublisher.publishEvent(new TarefaMarcadaComoAtrasada(tarefa));
            }
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
