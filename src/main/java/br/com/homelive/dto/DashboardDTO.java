package br.com.homelive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private long totalMoradores;
        private BigDecimal totalDespesasMes;
        private long totalTarefasPendentes;
        private long totalAgendamentosHoje;
        private List<MoradorDTO.Response> rankingMoradores;
        private List<AgendamentoDTO.Response> proximosAgendamentos;
    }
}
