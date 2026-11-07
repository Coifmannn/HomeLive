package br.com.homelive.service;

import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.dto.DashboardDTO;
import br.com.homelive.dto.MoradorDTO;
import br.com.homelive.model.enums.StatusAgendamento;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.repository.AgendamentoEspacoRepository;
import br.com.homelive.repository.DespesaRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.TarefaDomesticaRepository;
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

    public DashboardDTO.Response obterResumoGeral(Long casaId) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();

        long totalMoradores = moradorRepository.countByCasaIdAndStatusMembroAndAtivoTrue(casaId, StatusMembro.ATIVO);

        // Intervalos semiabertos [inicio, fim): não perdem a última fração do dia e distinguem o ano.
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        BigDecimal totalDespesasMes = despesaRepository.somarValorLancadoEntre(casaId, inicioMes, inicioMes.plusMonths(1));

        long totalTarefasPendentes = tarefaRepository.countByCasaIdAndStatus(casaId, StatusTarefa.PENDENTE);

        long totalAgendamentosHoje = agendamentoRepository
                .countByEspacoComumCasaIdAndStatusAndDataHoraInicioGreaterThanEqualAndDataHoraInicioLessThan(
                        casaId, StatusAgendamento.CONFIRMADO, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay());

        List<MoradorDTO.Response> ranking = moradorService.obterRankingGamificacao(casaId);

        List<AgendamentoDTO.Response> proximos = agendamentoRepository
                .findTop5ByEspacoComumCasaIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(casaId, StatusAgendamento.CONFIRMADO, agora)
                .stream()
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
