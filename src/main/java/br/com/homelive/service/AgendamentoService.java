package br.com.homelive.service;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.exception.ConflitoHorarioException;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.StatusAgendamento;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import lombok.RequiredArgsConstructor;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public AgendamentoDTO.Response criarAgendamento(AgendamentoDTO.Request request) {
        LocalDateTime inicio = request.getDataHoraInicio();
        LocalDateTime fim = request.getDataHoraFim();

        // 1. Validações temporais básicas
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("As datas e horários de início e término são obrigatórios.");
        }

        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("O horário de término deve ser posterior ao horário de início.");
        }

        if (inicio.isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é permitido agendar reservas em horários passados.");
        }

        // 2. Buscar entidades relacionadas e verificar status ativo
        EspacoComum espaco = espacoComumService.buscarEntidade(request.getEspacoId());
        if (!espaco.isAtivo()) {
            throw new RegraNegocioException("O espaço '" + espaco.getNome() + "' está temporariamente inativo para reservas.");
        }

        Morador morador = moradorService.buscarEntidade(request.getMoradorId());
        if (!morador.isAtivo()) {
            throw new RegraNegocioException("O morador " + morador.getNome() + " está inativo no sistema.");
        }

        // 3. Validação rigorosa de conflito de horários (POO e Integridade)
        List<AgendamentoEspaco> conflitos = agendamentoRepository.findConflitos(espaco.getId(), inicio, fim);
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

        // 4. Salvar agendamento
        AgendamentoEspaco agendamento = new AgendamentoEspaco(
                espaco,
                morador,
                inicio,
                fim,
                request.getMotivoReserva()
        );

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

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Este agendamento já se encontra cancelado.");
        }

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
