package br.com.homelive.dto;

import br.com.homelive.model.enums.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TarefaDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O título da tarefa é obrigatório")
        private String titulo;

        private String descricao;

        @NotNull(message = "O prazo da tarefa é obrigatório")
        private LocalDate dataPrazo;

        private Integer pontosRecompensa;

        private Integer pontosPenalidade;

        private Long responsavelId;

        private Long atribuidaPorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String titulo;
        private String descricao;
        private LocalDate dataPrazo;
        private Integer pontosRecompensa;
        private Integer pontosPenalidade;
        private StatusTarefa status;
        private Long responsavelId;
        private String responsavelNome;
        private String atribuidaPorNome;
        private LocalDateTime dataConclusao;
    }
}
