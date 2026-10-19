package br.com.homelive.dto;

import br.com.homelive.model.enums.StatusAgendamento;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class AgendamentoDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "O ID do espaço comum é obrigatório")
        private Long espacoId;

        /** Só o síndico informa (reserva em nome de um morador). Para morador, vale sempre o usuário logado. */
        private Long moradorId;

        @NotNull(message = "A data e hora de início são obrigatórias")
        @FutureOrPresent(message = "O início deve ser no presente ou futuro")
        private LocalDateTime dataHoraInicio;

        @NotNull(message = "A data e hora de término são obrigatórias")
        private LocalDateTime dataHoraFim;

        private String motivoReserva;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long espacoId;
        private String espacoNome;
        private Long moradorId;
        private String moradorNome;
        private String moradorQuarto;
        private LocalDateTime dataHoraInicio;
        private LocalDateTime dataHoraFim;
        private String motivoReserva;
        private StatusAgendamento status;
        private LocalDateTime dataCriacao;
    }
}
