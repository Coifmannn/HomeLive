package br.com.homelive.service;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.exception.ConflitoHorarioException;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.Morador;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoEspacoRepository agendamentoRepository;
    private final EspacoComumService espacoComumService;
    private final MoradorService moradorService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public AgendamentoDTO.Response criarAgendamento(AgendamentoDTO.Request request) {
        EspacoComum espaco = espacoComumService.buscarEntidade(request.getEspacoId());
        Morador morador = moradorService.buscarEntidade(request.getMoradorId());

        // As invariantes da reserva (datas, passado, espaço/morador ativos) são validadas pelo próprio construtor.
        AgendamentoEspaco agendamento = new AgendamentoEspaco(
                espaco,
                morador,
                request.getDataHoraInicio(),
                request.getDataHoraFim(),
                request.getMotivoReserva()
        );

        // Conflito depende de outras reservas, então só o service (com acesso ao repositório) consegue checar.
        List<AgendamentoEspaco> conflitos = agendamentoRepository.findConflitos(
                espaco.getId(), agendamento.getDataHoraInicio(), agendamento.getDataHoraFim());
        if (!conflitos.isEmpty()) {
            AgendamentoEspaco c = conflitos.get(0);
            throw new ConflitoHorarioException(String.format(
                    "Conflito de horário! O espaço '%s' já está reservado por %s de %s até %s.",
                    espaco.getNome(),
                    c.getMorador().getNome(),
                    c.getDataHoraInicio().format(FORMATTER),
                    c.getDataHoraFim().format(FORMATTER)
            ));
        }

        AgendamentoEspaco salvo = agendamentoRepository.save(agendamento);
        return toResponse(salvo);
    }

    public List<AgendamentoDTO.Response> listarTodos() {
        return agendamentoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<AgendamentoDTO.Response> listarPorEspaco(Long espacoId) {
        return agendamentoRepository.findByEspacoComumId(espacoId).stream().map(this::toResponse).toList();
    }

    public List<AgendamentoDTO.Response> listarPorMorador(Long moradorId) {
        return agendamentoRepository.findByMoradorId(moradorId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancelarAgendamento(Long agendamentoId) {
        AgendamentoEspaco agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + agendamentoId));

        agendamento.cancelar();
        agendamentoRepository.save(agendamento);
    }

    public AgendamentoDTO.Response toResponse(AgendamentoEspaco a) {
        return new AgendamentoDTO.Response(
                a.getId(),
                a.getEspacoComum().getId(),
                a.getEspacoComum().getNome(),
                a.getMorador().getId(),
                a.getMorador().getNome(),
                a.getMorador().getNumeroQuarto(),
                a.getDataHoraInicio(),
                a.getDataHoraFim(),
                a.getMotivoReserva(),
                a.getStatus(),
                a.getDataCriacao()
        );
    }
}
