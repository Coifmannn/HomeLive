package br.com.coliving.service;

import br.com.coliving.dto.AgendamentoDTO;
import br.com.coliving.dto.DashboardDTO;
import br.com.coliving.dto.MoradorDTO;
import br.com.coliving.model.Despesa;
import br.com.coliving.model.enums.StatusAgendamento;
import br.com.coliving.model.enums.StatusTarefa;
import br.com.coliving.repository.AgendamentoEspacoRepository;
import br.com.coliving.repository.DespesaRepository;
import br.com.coliving.repository.MoradorRepository;
import br.com.coliving.repository.TarefaDomesticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MoradorRepository moradorRepository;
    private final DespesaRepository despesaRepository;
    private final TarefaDomesticaRepository tarefaRepository;
    private final AgendamentoEspacoRepository agendamentoRepository;
    private final MoradorService moradorService;
    private final AgendamentoService agendamentoService;

    public DashboardDTO.Response obterResumoGeral() {
        long totalMoradores = moradorRepository.count();

        BigDecimal totalDespesasMes = despesaRepository.findAll().stream()
                .filter(d -> d.getDataLancamento().getMonth() == LocalDate.now().getMonth())
                .map(Despesa::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTarefasPendentes = tarefaRepository.findByStatus(StatusTarefa.PENDENTE).size();

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDia = LocalDate.now().atTime(23, 59, 59);

        long totalAgendamentosHoje = agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO &&
                             !a.getDataHoraInicio().isBefore(inicioDia) &&
                             !a.getDataHoraInicio().isAfter(fimDia))
                .count();

        List<MoradorDTO.Response> ranking = moradorService.obterRankingGamificacao();

        List<AgendamentoDTO.Response> proximos = agendamentoRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONFIRMADO && a.getDataHoraInicio().isAfter(LocalDateTime.now()))
                .sorted((a1, a2) -> a1.getDataHoraInicio().compareTo(a2.getDataHoraInicio()))
                .limit(5)
                .map(agendamentoService::toResponse)
                .toList();

        return new DashboardDTO.Response(
                totalMoradores,
                totalDespesasMes,
                totalTarefasPendentes,
                totalAgendamentosHoje,
                ranking,
                proximos
        );
    }
}
