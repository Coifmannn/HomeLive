package br.com.homelive.service;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.event.MoradorNaoCompareceu;
import br.com.homelive.event.ReservaCanceladaPeloMorador;
import br.com.homelive.exception.ConflitoHorarioException;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.Morador;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoEspacoRepository agendamentoRepository;
    private final EspacoComumService espacoComumService;
    private final MoradorService moradorService;
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public AgendamentoDTO.Response criarAgendamento(AgendamentoDTO.Request request, UsuarioAutenticado ator) {
        Long moradorId = ator.moradorAlvo(request.getMoradorId());
        EspacoComum espaco = espacoComumService.buscarEntidade(request.getEspacoId());
        Morador morador = moradorService.buscarEntidade(moradorId);

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
    public void cancelarAgendamento(Long agendamentoId, UsuarioAutenticado ator) {
        AgendamentoEspaco agendamento = buscarEntidade(agendamentoId);

        ator.exigirSindicoOuDono(agendamento.getMorador().getId());

        agendamento.cancelar();
        agendamentoRepository.save(agendamento);

        // Só o cancelamento feito pelo próprio morador pode virar infração (o do síndico é administrativo).
        if (!ator.isSindico()) {
            eventPublisher.publishEvent(new ReservaCanceladaPeloMorador(agendamento, LocalDateTime.now()));
        }
    }

    @Transactional
    public AgendamentoDTO.Response registrarNaoComparecimento(Long agendamentoId) {
        AgendamentoEspaco agendamento = buscarEntidade(agendamentoId);

        agendamento.registrarNaoComparecimento(LocalDateTime.now());
        agendamentoRepository.save(agendamento);
        eventPublisher.publishEvent(new MoradorNaoCompareceu(agendamento));
        return toResponse(agendamento);
    }

    private AgendamentoEspaco buscarEntidade(Long agendamentoId) {
        return agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + agendamentoId));
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
